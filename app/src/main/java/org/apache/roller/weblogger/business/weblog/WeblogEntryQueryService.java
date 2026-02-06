package org.apache.roller.weblogger.business.weblog;

import java.util.Collections;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;

public class WeblogEntryQueryService {

    private static final Log log = LogFactory.getLog(WeblogEntryQueryService.class);
    private static final int MAX_ENTRIES = 100;

    public WeblogEntry getWeblogEntry(Weblog weblog, String anchor) {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            return wmgr.getWeblogEntryByAnchor(weblog, anchor);
        } catch (WebloggerException e) {
            log.error("ERROR: getting entry by anchor", e);
        }
        return null;
    }

    public List<WeblogEntry> getRecentEntries(Weblog weblog, int max) {
        if (max < 1) {
            return Collections.emptyList();
        }
        if (max > MAX_ENTRIES) {
            max = MAX_ENTRIES;
        }
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
            wesc.setWeblog(weblog);
            wesc.setStatus(PubStatus.PUBLISHED);
            wesc.setMaxResults(max);
            return wmgr.getWeblogEntries(wesc);
        } catch (WebloggerException e) {
            log.error("ERROR: getting recent entries", e);
        }
        return Collections.emptyList();
    }

    public WeblogCategory getWeblogCategory(Weblog weblog, String name) {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            if (name != null && !"nil".equals(name)) {
                return wmgr.getWeblogCategoryByName(weblog, name);
            }
            return weblog.getWeblogCategories().iterator().next();
        } catch (WebloggerException e) {
            log.error("ERROR: fetching category: " + name, e);
        }
        return null;
    }

    public List<WeblogEntry> getRecentWeblogEntries(Weblog weblog, String cat, int length) {
        if (cat != null && "nil".equals(cat)) {
            cat = null;
        }
        if (length > MAX_ENTRIES) {
            length = MAX_ENTRIES;
        }
        if (length < 1) {
            return Collections.emptyList();
        }
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();
            WeblogEntrySearchCriteria wesc = new WeblogEntrySearchCriteria();
            wesc.setWeblog(weblog);
            wesc.setCatName(cat);
            wesc.setStatus(PubStatus.PUBLISHED);
            wesc.setMaxResults(length);
            return wmgr.getWeblogEntries(wesc);
        } catch (WebloggerException e) {
            log.error("ERROR: getting recent entries", e);
        }
        return Collections.emptyList();
    }

}
