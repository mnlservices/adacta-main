package nl.defensie.local;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import nl.defensie.adacta.service.Adacta2DatabaseService;

/**
 * A valid database connection configuration is required to run these tests.
 * TODO: add setup that can prepare a database.
 * @author Mark Tielemans
 */

// TODO - must be refactored - the DISP8 database is not in use anymore


//public class Adacta2DatabaseServiceTest extends LocalSpringTest {
public class Adacta2DatabaseServiceTest {
    private static final String TEST_EMPLID = "00000097874";
    private static final Logger logger = Logger.getLogger(Adacta2DatabaseServiceTest.class);

  //  private static Adacta2DatabaseService adacta2DatabaseService;

    @Before
    public void before() {
    //    adacta2DatabaseService = testContext.getAdacta2DatabaseService();
    }

  
    @Ignore
    public void getUserDPCodesTest() {
        logger.debug("Fetching DPCODES for " + TEST_EMPLID);
   //     final List<Gebruiker> users = adactaDatabaseService.getUserDPCodes(TEST_EMPLID);
    //    Assert.assertEquals(1, users.size());
     //   Assert.assertEquals(TEST_EMPLID, users.get(0).getSTREMPID());
    }
}
