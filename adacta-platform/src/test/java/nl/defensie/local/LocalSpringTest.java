package nl.defensie.local;

import org.junit.AfterClass;
import org.junit.runner.RunWith;

/**
 * <p>Defines test cases that run in their own test Spring context, without having to 
 * start usual heavyweight dependencies such as Alfresco's context. Be careful to use this in 
 * a way that the tests are still relevant.</p>
 * 
 * <p>The testContext provides access to the test context's beans and services, 
 * and should be provided by the runner at test creation.</p>
 * 
 * @see LocalSpringRunner
 * @author mark.tielemans
 */
//@RunWith(LocalSpringRunner.class)
public abstract class LocalSpringTest {
    protected static TestContext testContext;
    
    public static void setTestContext(final TestContext testContext) {
        LocalSpringTest.testContext = testContext;
    }
    
    @AfterClass
    public static void tearDown() {
        LocalSpringRunner.shutdown();
    }
}
