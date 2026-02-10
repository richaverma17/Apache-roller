package org.apache.roller.weblogger.business;

import java.util.Collections;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.CommentSearchCriteria;
import org.apache.roller.weblogger.pojos.GlobalPermission;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogPermission;
import java.util.Calendar;
import java.util.Date;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;

public class WeblogEntryAccess {
    private static final Log LOG = LogFactory.getLog(WeblogEntryAccess.class);

    private WeblogEntryAccess() {
    }

    public static User getCreator(WeblogEntry entry) {
        try {
            return WebloggerFactory.getWeblogger()
                    .getUserManager()
                    .getUserByUserName(entry.getCreatorUserName());
        } catch (Exception e) {
            LOG.error("ERROR fetching user object for username: "
                    + entry.getCreatorUserName(), e);
        }
        return null;
    }

    public static List<WeblogEntryComment> getComments(WeblogEntry entry, boolean approvedOnly) {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger()
                    .getWeblogEntryManager();

            CommentSearchCriteria csc = new CommentSearchCriteria();
            csc.setWeblog(entry.getWebsite());
            csc.setEntry(entry);
            csc.setStatus(approvedOnly ? WeblogEntryComment.ApprovalStatus.APPROVED : null);
            return wmgr.getComments(csc);
        } catch (WebloggerException alreadyLogged) {
        }

        return Collections.emptyList();
    }

    public static String getPermalink(WeblogEntry entry) {
        return WebloggerFactory.getWeblogger()
                .getUrlStrategy()
                .getWeblogEntryURL(entry.getWebsite(), null, entry.getAnchor(), true);
    }

    public static String createAnchor(WeblogEntry entry) throws WebloggerException {
        return WebloggerFactory.getWeblogger()
                .getWeblogEntryManager()
                .createAnchor(entry);
    }

    public static boolean hasWritePermissions(WeblogEntry entry, User user)
            throws WebloggerException {

        GlobalPermission adminPerm =
                new GlobalPermission(Collections.singletonList(GlobalPermission.ADMIN));
        boolean hasAdmin = WebloggerFactory.getWeblogger()
                .getUserManager()
                .checkPermission(adminPerm, user);
        if (hasAdmin) {
            return true;
        }

        WeblogPermission perm;
        try {
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            perm = umgr.getWeblogPermission(entry.getWebsite(), user);

        } catch (WebloggerException ex) {
            LOG.error("ERROR retrieving user's permission", ex);
            return false;
        }

        boolean author = perm.hasAction(WeblogPermission.POST)
                || perm.hasAction(WeblogPermission.ADMIN);
        boolean limited = !author && perm.hasAction(WeblogPermission.EDIT_DRAFT);

        return author || (limited
                && (entry.getStatus() == WeblogEntry.PubStatus.DRAFT
                || entry.getStatus() == WeblogEntry.PubStatus.PENDING));
    }




    public static boolean getCommentsStillAllowed(WeblogEntry entry) {
        if (!WebloggerRuntimeConfig.getBooleanProperty("users.comments.enabled")) {
            return false;
        }
        if (entry.getWebsite().getAllowComments() != null
                && !entry.getWebsite().getAllowComments()) {
            return false;
        }
        if (entry.getAllowComments() != null && !entry.getAllowComments()) {
            return false;
        }
        boolean ret = false;
        if (entry.getCommentDays() == null || entry.getCommentDays() == 0) {
            ret = true;
        } else {
            Date inPubTime = entry.getPubTime();
            if (inPubTime == null) {
                inPubTime = entry.getUpdateTime();
            }

            Calendar expireCal = Calendar.getInstance(
                    entry.getWebsite().getLocaleInstance());
            expireCal.setTime(inPubTime);
            expireCal.add(Calendar.DATE, entry.getCommentDays());
            Date expireDay = expireCal.getTime();
            Date today = new Date();
            if (today.before(expireDay)) {
                ret = true;
            }
        }
        return ret;
    }

}
