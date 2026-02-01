package org.apache.roller.weblogger.business;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.inject.Inject;

import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.pings.AutoPingManager;
import org.apache.roller.weblogger.business.pings.PingQueueManager;
import org.apache.roller.weblogger.business.pings.PingTargetManager;
import org.apache.roller.weblogger.config.PingConfig;

@com.google.inject.Singleton
public class PingManagerFacade {

    private static final Log log = LogFactory.getLog(PingManagerFacade.class);

    private final AutoPingManager  autoPingManager;
    private final PingQueueManager pingQueueManager;
    private final PingTargetManager pingTargetManager;

    @Inject
    public PingManagerFacade(
            AutoPingManager  autoPingManager,
            PingQueueManager pingQueueManager,
            PingTargetManager pingTargetManager) {
        this.autoPingManager  = autoPingManager;
        this.pingQueueManager = pingQueueManager;
        this.pingTargetManager = pingTargetManager;
    }

    public AutoPingManager  getAutoPingManager()  { return autoPingManager; }
    public PingQueueManager getPingQueueManager() { return pingQueueManager; }
    public PingTargetManager getPingTargetManager() { return pingTargetManager; }

    public void initialize() throws InitializationException {
        try {
            PingConfig.initializeCommonTargets();
            PingConfig.initializePingVariants();

            if (PingConfig.getDisablePingUsage()) {
                log.info("Ping usage disabled → removing auto-ping configurations");
                autoPingManager.removeAllAutoPings();
            }
        } catch (Exception e) {
            throw new InitializationException("Ping subsystem initialization failed", e);
        }
    }

    public void release() {
        try {
            autoPingManager.release();
            pingQueueManager.release();
            pingTargetManager.release();
        } catch (Exception e) {
            log.error("Error releasing ping managers", e);
        }
    }

    public void shutdown() {
        // No specific shutdown logic in original code
    }
}