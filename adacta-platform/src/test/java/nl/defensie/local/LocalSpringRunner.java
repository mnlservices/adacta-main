package nl.defensie.local;

import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * JUnit test runner that can start a Spring context for testing and provide it to 
 * the test cases, which must be {@link LocalSpringTest} cases.
 * @author mark.tielemans
 */
public class LocalSpringRunner extends BlockJUnit4ClassRunner {

    private static ClassPathXmlApplicationContext app;
    private TestContext ctxt; 

    public LocalSpringRunner(final Class<?> klass) throws InitializationError {
        super(klass);

        // Start Spring context
   //     System.setProperty("log4j.configuration", "file:src/test/resources/test-log4j.properties");
   //     app = new ClassPathXmlApplicationContext("alfresco/test-context/spring-context.xml");
        
        // Provide context to test case
    //    ctxt = (TestContext) app.getBean("testContext");
    //    LocalSpringTest.testContext = ctxt;
    }

    public static void shutdown() {
        if (app != null) {
            app.close();
            app = null;
        }
    }
}
