package org.apache.roller.weblogger.business.weblog;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogTheme;


public class WeblogThemeService {
    private static final Log log =
            LogFactory.getLog(WeblogThemeService.class);
    public WeblogTheme getTheme(Weblog weblog) {
        try {
            // let the ThemeManager handle it
            ThemeManager themeMgr = WebloggerFactory.getWeblogger().getThemeManager();
            return themeMgr.getTheme(weblog);
        } catch (WebloggerException ex) {
            log.error("Error getting theme for weblog - " + weblog.getHandle(), ex);
        }

        // TODO: maybe we should return a default theme in this case?
        return null;
    }
}
