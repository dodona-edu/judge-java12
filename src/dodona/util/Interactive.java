package dodona.util;

import dodona.junit.ExitException;
import dodona.junit.MultiMessageWriter;
import dodona.junit.TestWriter;
import org.junit.Assert;
import org.junit.contrib.java.lang.system.SystemOutRule;
import org.junit.contrib.java.lang.system.TextFromStandardInputStream;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.contrib.java.lang.system.TextFromStandardInputStream.emptyStandardInputStream;

/**
 * Allows an interactive application to be tested conveniently.
 */
public class Interactive implements TestRule {
    private final Class<?> cls;
    
    // TextFromStandardInputStream by default only provides input to the stream
    // once.
    private final Collection<String> inputLines = new ArrayList<>();
    
    // The id of the feedback message that contains the contents of stdout.
    private int outputMessageId = -1;
    
    // Rules that are not treated as rules.
    private final TestWriter diff = new TestWriter();
    private final MultiMessageWriter feedback = new MultiMessageWriter();
    private final TextFromStandardInputStream stdin = emptyStandardInputStream();
    private final SystemOutRule stdout = new SystemOutRule().enableLog().mute();
    
    /**
     * Interactive constructor.
     *
     * @param cls the class to test
     */
    private Interactive(final Class<?> cls) {
        this.cls = cls;
    }
    
    @Override
    public Statement apply(final Statement base, final Description description) {
        // Ugly hack to use existing Rules as Rules.
        return this.feedback.apply(this.diff.apply(this.stdin.apply(
            this.stdout.apply(base, description),
            description),
            description),
            description);
    }
    
    /**
     * Asserts that the contents of stdout match the expected value.
     *
     * @param expected the expected output
     * @return fluent
     */
    public Interactive assertOutput(final String expected) {
        // Remove the output from the feedback stream if any.
        if (this.outputMessageId > -1) {
            this.feedback.remove(this.outputMessageId);
            this.outputMessageId = -1;
        }
        // Perform the comparison.
        this.diff.compare(expected, this.output());
        return this;
    }
    
    /**
     * Allow messages to be appended in TestCarryingThrowable
     * Calls the .main()-method of the class under test.
     *
     * @param args commandline arguments
     * @return fluent
     * @throws Throwable exception thrown by the program
     */
    public Interactive callMain(final String... args) throws Throwable {
        // Clear anything on stdout before the test starts.
        this.stdout.clearLog();
        
        // Feed the input stream.
        this.stdin.provideLines(this.inputLines.toArray(new String[]{}));
        
        // Log the input stream.
        this.logInput();
        
        // Execute the main method.
        try {
            final Method main = this.cls.getMethod("main", String[].class);
            main.invoke(null, (Object) args);
        } catch (final NoSuchMethodException e) {
            Assert.fail("Method not found: public static void main(String[])");
        } catch (final IllegalAccessException e) {
            Assert.fail("Method could not be called: public static void main(String[])");
        } catch (final InvocationTargetException e) {
            // An exception occurred while running the program. Ignore this if
            // it's because of a call to System.exit(), as this might be desired
            if (!(e.getCause() instanceof ExitException)) {
                throw e.getCause();
            }
        } finally {
            // Log the output.
            this.logOutput();
        }
        
        // Fluent.
        return this;
    }
    
    /**
     * @deprecated Use {@link #feedLines(double...)} instead.
     */
    @Deprecated
    public Interactive feedLine(final double... args) {
        return this.feedLines(args);
    }
    
    /**
     * Feeds double arguments to stdin, each on a separate line.
     *
     * @param args the doubles to send to stdin
     * @return fluent
     */
    public Interactive feedLines(final double... args) {
        Arrays.stream(args).mapToObj(Double::toString).forEach(this::feedLine);
        return this;
    }
    
    /**
     * @deprecated Use {@link #feedLines(int...)} instead.
     */
    @Deprecated
    public Interactive feedLine(final int... args) {
        return this.feedLines(args);
    }
    
    /**
     * Feeds integer arguments to stdin, each on a separate line.
     *
     * @param args the integers to send to stdin
     * @return fluent
     */
    public Interactive feedLines(final int... args) {
        Arrays.stream(args).mapToObj(Integer::toString).forEach(this::feedLine);
        return this;
    }
    
    /**
     * @deprecated Use {@link #feedLines(String...)} instead.
     */
    @Deprecated
    public Interactive feedLine(final String... args) {
        return this.feedLines(args);
    }
    
    /**
     * Feeds string arguments to stdin, each on a separate line.
     *
     * @param args the text to send to stdin
     * @return fluent
     */
    public Interactive feedLines(final String... args) {
        Arrays.stream(args).forEach(this::feedLine);
        return this;
    }
    
    /**
     * Feeds one input line to stdin.
     *
     * @param line the input line
     */
    private void feedLine(final String line) {
        this.inputLines.add(line);
    }
    
    /**
     * Gets an Interactive-instance for the given class.
     *
     * @param cls the class to interact with
     * @return test instance
     */
    public static Interactive forClass(final Class<?> cls) {
        return new Interactive(cls);
    }
    
    /**
     * Logs the contents of stdin to the feedback stream.
     */
    private void logInput() {
        this.feedback.append(String.format("Input:\n%s", String.join("\n", this.inputLines)));
    }
    
    /**
     * Logs the contents of stdout to the feedback stream.
     */
    private void logOutput() {
        this.outputMessageId = this.feedback.append(String.format("Output:\n%s", this.output()));
    }
    
    /**
     * Gets the contents of stdout as a string.
     *
     * @return the output
     */
    public String output() {
        return this.stdout.getLogWithNormalizedLineSeparator().trim();
    }
    
    /**
     * Gets the contents of stdout as an integer. If the output can not be
     * parsed (or is empty), the test will fail.
     *
     * @return the output parsed as an integer
     */
    public int outputAsInteger() {
        final String output = this.output();
        
        // Attempt to parse the string as an integer.
        try {
            return Integer.parseInt(output);
        } catch (final Exception ex) {
            if (output.isEmpty()) {
                Assert.fail("The application did not produce any output.");
            } else {
                Assert.fail(String.format("The output could not be parsed as an integer: %s", output));
            }
            
            // Unreachable.
            return -1;
        }
    }
    
    /**
     * Gets the contents of stdout as an array of lines.
     *
     * @return the output, split on newlines
     */
    public String[] outputLines() {
        return this.stdout.getLogWithNormalizedLineSeparator().trim().split("\n");
    }
}