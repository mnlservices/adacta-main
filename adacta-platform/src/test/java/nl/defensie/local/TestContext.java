package nl.defensie.local;

import org.springframework.beans.factory.annotation.Autowired;

import nl.defensie.adacta.service.Adacta2DatabaseService;

/**
 * Creates a local Spring test context.
 * @author mark.tielemans
 */
public class TestContext {

    private static TestContext instance;
    
    @Autowired
    private Adacta2DatabaseService adacta2DatabaseService;
    
    /**
     * @deprecated don't manually create. Required for Spring initialization.
     */
    public TestContext() {}
    
    public Adacta2DatabaseService getAdacta2DatabaseService() {
        return adacta2DatabaseService;
    }

    public static TestContext getInstance() {
        return instance;
    }
    
    public static void setInstance(final TestContext instance) {
        TestContext.instance = instance;
    }
}
