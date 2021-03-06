import org.junit.Assert;
import org.junit.Test;
import org.junit.Before;

import dodona.reflection.AssertionStubber;

public class StubberTest {

    private StubberInterface solution;

    @Before public void initialize() {
        solution = new AssertionStubber().stub(StubberInterface.class, Stubber.class);
    }
        
    @Test
    public void test() {
        Assert.assertTrue(solution.isStubbed());
    }

}
