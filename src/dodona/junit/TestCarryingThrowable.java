package dodona.junit;

import dodona.feedback.CloseTest;
import dodona.feedback.Message;
import dodona.feedback.StartTest;

import java.util.Collections;
import java.util.List;

public class TestCarryingThrowable extends Throwable {
    private static final long serialVersionUID = 1L;
    
    private final CloseTest closeTest;
    private final StartTest startTest;
    
    private final List<Message> messages;
    
    /**
     * TestCarryingThrowable constructor.
     *
     * @param cause     the underlying error cause
     * @param startTest the start command of the test
     * @param closeTest the end command of the test
     */
    public TestCarryingThrowable(final Throwable cause, final StartTest startTest,
                                 final CloseTest closeTest) {
        this(cause, startTest, Collections.emptyList(), closeTest);
    }
    
    /**
     * TestCarryingThrowable constructor.
     *
     * @param cause     the underlying error cause
     * @param startTest the start command of the test
     * @param messages  additional messages to include in the test
     * @param closeTest the end command of the test
     */
    public TestCarryingThrowable(final Throwable cause, final StartTest startTest,
                                 final List<Message> messages,
                                 final CloseTest closeTest) {
        super(cause);
        this.closeTest = closeTest;
        this.messages = messages;
        this.startTest = startTest;
    }
    
    public CloseTest getCloseTest() {
        return this.closeTest;
    }
    
    /**
     * Gets the feedback messages.
     *
     * @return the feedback messages
     */
    public List<Message> getMessages() {
        return this.messages;
    }
    
    public StartTest getStartTest() {
        return this.startTest;
    }
}
