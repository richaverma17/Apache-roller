package org.apache.roller.weblogger.business.weblog;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.plugins.PluginManager;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.pojos.Weblog;

public class WeblogPluginService {

    private static final Log log = LogFactory.getLog(WeblogPluginService.class);

    public Map<String, WeblogEntryPlugin> getInitializedPlugins(Weblog weblog) {
        try {
            PluginManager ppmgr = WebloggerFactory.getWeblogger().getPluginManager();
            return ppmgr.getWeblogEntryPlugins(weblog);
        } catch (Exception e) {
            log.error("ERROR: initializing plugins", e);
        }
        return null;
    }
}
