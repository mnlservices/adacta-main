package nl.defensie.adacta.action.schedule;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.alfresco.repo.action.executer.ActionExecuterAbstractBase;
import org.alfresco.service.cmr.action.Action;
import org.alfresco.service.cmr.action.ParameterDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;

import nl.defensie.adacta.service.AdactaDossierSyncService;

public class AdactaDossierSynchronizer extends ActionExecuterAbstractBase {

    private static final Log LOG = LogFactory.getLog(AdactaDossierSynchronizer.class);
    public static final String NAME = "adactaDossierSynchronizer";

    @Autowired
    protected AdactaDossierSyncService adactaDossierSyncService;
    
    @Override
    protected void executeImpl(final Action action, final NodeRef actionedUponNodeRef) {
        LOG.info("Start Adacta dossier synchronizer");

        final long start = System.nanoTime();

        Throwable throwable = null;
        try {
                adactaDossierSyncService.syncDossiers();
        } catch (final Throwable t) {
            throwable = t;
            throw t;
        } finally {
            final long seconds = TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - start);

            if (throwable != null) {
            	LOG.error(String.format("Ended with exception after %s seconds", seconds), throwable);
            	AdactaDossierSyncService.getSyncLogger().error(String.format("Ended with exception after %s seconds", seconds), throwable);
            } else {
            	LOG.error(String.format("Ended with exception after %s seconds", seconds));
                AdactaDossierSyncService.getSyncLogger().info(String.format("Ended after %s seconds", seconds));
            }
        }
    }

    @Override
    protected void addParameterDefinitions(final List<ParameterDefinition> paramList) {
    }
}
