/*
* Licensed to the Apache Software Foundation (ASF) under one or more
*  contributor license agreements.  The ASF licenses this file to You
* under the Apache License, Version 2.0 (the "License"); you may not
* use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.  For additional information regarding
* copyright in this work, please see the NOTICE file in the top level
* directory of this distribution.
*/

package org.apache.roller.weblogger.pojos;

import java.io.Serializable;
import java.util.*;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.util.UUIDGenerator;
import org.apache.roller.weblogger.util.I18nUtils;
import org.apache.roller.weblogger.util.Utilities;
import org.apache.roller.weblogger.business.weblog.WeblogThemeService;
import org.apache.roller.weblogger.business.weblog.WeblogPluginService;
import org.apache.roller.weblogger.business.WeblogService;


/**
 * Website has many-to-many association with users. Website has one-to-many and
 * one-direction associations with weblog entries, weblog categories, folders and
 * other objects. Use UserManager to create, fetch, update and retrieve websites.
 *
 * REFACTORED: Service-layer methods that previously called WebloggerFactory directly
 * have been moved to WeblogService. This class now contains only data, utility methods,
 * and delegations to service wrapper classes (WeblogThemeService, WeblogPluginService, etc.)
 * Query methods delegate to WeblogService to avoid cyclic dependencies.
 *
 * @author David M Johnson
 */
public class Weblog implements Serializable {
    
    public static final long serialVersionUID = 206437645033737127L;
    
    private static Log log = LogFactory.getLog(Weblog.class);

    private static final int MAX_ENTRIES = 100;
    
    // Simple properties
    private String  id               = UUIDGenerator.generateUUID();
    private String  handle           = null;
    private String  name             = null;
    private String  tagline          = null;
    private Boolean enableBloggerApi = Boolean.TRUE;
    private String  editorPage       = null;
    private String  bannedwordslist  = null;
    private Boolean allowComments    = Boolean.TRUE;
    private Boolean emailComments    = Boolean.FALSE;
    private String  emailAddress     = null;
    private String  editorTheme      = null;
    private String  locale           = null;
    private String  timeZone         = null;
    private String  defaultPlugins   = null;
    private Boolean visible          = Boolean.TRUE;
    private Boolean active           = Boolean.TRUE;
    private Date    dateCreated      = new java.util.Date();
    private Boolean defaultAllowComments = Boolean.TRUE;
    private int     defaultCommentDays = 0;
    private Boolean moderateComments = Boolean.FALSE;
    private int     entryDisplayCount = 15;
    private Date    lastModified     = new Date();
    private boolean enableMultiLang  = false;
    private boolean showAllLangs     = true;
    private String  iconPath         = null;
    private String  about            = null;
    private String  creator          = null;
    private String  analyticsCode    = null;

    // Associated objects
    private WeblogCategory bloggerCategory = null;

    private Map<String, WeblogEntryPlugin> initializedPlugins = null;

    private List<WeblogCategory> weblogCategories = new ArrayList<>();

    private List<WeblogBookmarkFolder> bookmarkFolders = new ArrayList<>();

    private List<MediaFileDirectory> mediaFileDirectories = new ArrayList<>();

    public Weblog() {
    }
    
    public Weblog(
            String handle,
            String creator,
            String name,
            String desc,
            String email,
            String editorTheme,
            String locale,
            String timeZone) {
        
        this.handle = handle;
        this.creator = creator;
        this.name = name;
        this.tagline = desc;
        this.emailAddress = email;
        this.editorTheme = editorTheme;
        this.locale = locale;
        this.timeZone = timeZone;
    }
    
    //------------------------------------------------------- Good citizenship

    @Override
    public String toString() {
        return  "{" + getId() + ", " + getHandle()
        + ", " + getName() + ", " + getEmailAddress()
        + ", " + getLocale() + ", " + getTimeZone() + "}";
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof Weblog)) {
            return false;
        }
        Weblog o = (Weblog)other;
        return new EqualsBuilder()
            .append(getHandle(), o.getHandle()) 
            .isEquals();
    }
    
    @Override
    public int hashCode() { 
        return new HashCodeBuilder()
            .append(getHandle())
            .toHashCode();
    } 
    
    //------------------------------------------------------- Service wrapper methods
    // These delegate to service classes that don't create cyclic dependencies
    
    /**
     * Get the Theme object in use by this weblog, or null if no theme selected.
     * Delegates to WeblogThemeService.
     */
    public WeblogTheme getTheme() {
        return new WeblogThemeService().getTheme(this);
    }
    
    /**
     * Get initialized plugins for use during rendering process.
     * Delegates to WeblogPluginService.
     */
    public Map<String, WeblogEntryPlugin> getInitializedPlugins() {
        if (initializedPlugins == null) {
            initializedPlugins = new WeblogPluginService().getInitializedPlugins(this);
        }
        return initializedPlugins;
    }
    
    /** 
     * Get weblog entry specified by anchor or null if no such entry exists.
     * Delegates to WeblogService.
     */
    public WeblogEntry getWeblogEntry(String anchor) {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getWeblogEntry(this, anchor);
        } catch (WebloggerException e) {
            log.error("Error getting weblog entry by anchor: " + anchor, e);
            return null;
        }
    }

    /**
     * Get weblog category by name.
     * @deprecated This method is not currently supported through WeblogService.
     * Returns null for now. Category functionality may need to be added to WeblogService.
     */
    @Deprecated
    public WeblogCategory getWeblogCategory(String categoryName) {
        log.warn("getWeblogCategory() called but not implemented in WeblogService");
        return null;
    }
    
    /**
     * Get up to 100 most recent published entries in weblog.
     * Note: Category filtering is not currently supported. This method
     * returns all recent entries regardless of the category parameter.
     * 
     * @param cat Category name (currently ignored)
     * @param length Maximum number of entries to return
     * @return List of recent weblog entries
     */
    public List<WeblogEntry> getRecentWeblogEntries(String cat, int length) {
        try {
            // WeblogService doesn't support category filtering yet
            // So we ignore the cat parameter and get all recent entries
            if (cat != null) {
                log.warn("Category filtering requested but not supported in WeblogService");
            }
            return WebloggerFactory.getWeblogger().getWeblogService().getRecentEntries(this, length);
        } catch (WebloggerException e) {
            log.error("Error getting recent weblog entries", e);
            return Collections.emptyList();
        }
    }
    
    /**
     * Get up to 100 most recent published entries in weblog by tag.
     * Delegates to WeblogService.
     */
    public List<WeblogEntry> getRecentWeblogEntriesByTag(String tag, int length) {
        if (tag != null && "nil".equals(tag)) {
            tag = null;
        }
        if (length > MAX_ENTRIES) {
            length = MAX_ENTRIES;
        }
        if (length < 1) {
            return Collections.emptyList();
        }
        List<String> tags = Collections.emptyList();
        if (tag != null) {
            tags = List.of(tag);
        }
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getRecentEntriesWithTags(this, tags, length);
        } catch (WebloggerException e) {
            log.error("Error getting recent weblog entries by tag", e);
            return Collections.emptyList();
        }
    }
    
    //------------------------------------------------------- Methods using WeblogService
    // These were moved from direct WebloggerFactory calls to WeblogService
    
    /**
     * Original creator of website.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public org.apache.roller.weblogger.pojos.User getCreator() {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getCreator(this);
        } catch (Exception e) {
            log.error("ERROR fetching user object for username: " + creator, e);
        }
        return null;
    }
    
    /**
     * Get up to 100 most recent approved and non-spam comments in weblog.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public List<WeblogEntryComment> getRecentComments(int length) {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getRecentComments(this, length);
        } catch (Exception e) {
            log.error("ERROR: getting recent comments", e);
        }
        return Collections.emptyList();
    }

    /**
     * Get bookmark folder by name.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public WeblogBookmarkFolder getBookmarkFolder(String folderName) {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getBookmarkFolder(this, folderName);
        } catch (Exception e) {
            log.error("ERROR: fetching folder for weblog", e);
        }
        return null;
    }

    /**
     * Get number of hits counted today.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public int getTodaysHits() {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getTodaysHits(this);
        } catch (Exception e) {
            log.error("Error getting weblog hit count", e);
        }
        return 0;
    }

    /**
     * Get a list of TagStats objects for the most popular tags.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public List<TagStat> getPopularTags(int sinceDays, int length) {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getPopularTags(this, sinceDays, length);
        } catch (Exception e) {
            log.error("ERROR: fetching popular tags for weblog " + this.getName(), e);
        }
        return Collections.emptyList();
    }

    /**
     * Get comment count for this weblog.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public long getCommentCount() {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getCommentCount(this);
        } catch (Exception e) {
            log.error("Error getting comment count for weblog " + this.getName(), e);
        }
        return 0;
    }
    
    /**
     * Get entry count for this weblog.
     * NOTE: This method uses WeblogService to avoid cyclic dependency.
     */
    public long getEntryCount() {
        try {
            return WebloggerFactory.getWeblogger().getWeblogService().getEntryCount(this);
        } catch (Exception e) {
            log.error("Error getting entry count for weblog " + this.getName(), e);
        }
        return 0;
    }

    //------------------------------------------------------- Getters and Setters

    /**
     * Id of the Website.
     */
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Short URL safe string that uniquely identifies the website.
     */
    public String getHandle() {
        return this.handle;
    }
    
    public void setHandle(String handle) {
        this.handle = handle;
    }
    
    /**
     * Name of the Website.
     */
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = Utilities.removeHTML(name);
    }
    
    /**
     * Description
     */
    public String getTagline() {
        return this.tagline;
    }
    
    public void setTagline(String tagline) {
        this.tagline = Utilities.removeHTML(tagline);
    }
    
    /**
     * Username of original creator of website.
     */
    public String getCreatorUserName() {
        return creator;
    }
    
    public void setCreatorUserName(String creatorUserName) {
        creator = creatorUserName;
    }

    public Boolean getEnableBloggerApi() {
        return this.enableBloggerApi;
    }
    
    public void setEnableBloggerApi(Boolean enableBloggerApi) {
        this.enableBloggerApi = enableBloggerApi;
    }
    
    public WeblogCategory getBloggerCategory() { 
        return bloggerCategory; 
    }
    
    public void setBloggerCategory(WeblogCategory bloggerCategory) {
        this.bloggerCategory = bloggerCategory;
    }

    public String getEditorPage() {
        return this.editorPage;
    }
    
    public void setEditorPage(String editorPage) {
        this.editorPage = editorPage;
    }
    
    public String getBannedwordslist() {
        return this.bannedwordslist;
    }
    
    public void setBannedwordslist(String bannedwordslist) {
        this.bannedwordslist = bannedwordslist;
    }
    
    public Boolean getAllowComments() {
        return this.allowComments;
    }
    
    public void setAllowComments(Boolean allowComments) {
        this.allowComments = allowComments;
    }
    
    public Boolean getEmailComments() {
        return this.emailComments;
    }
    
    public void setEmailComments(Boolean emailComments) {
        this.emailComments = emailComments;
    }
    
    public String getEmailAddress() {
        return this.emailAddress;
    }
    
    public void setEmailAddress(String emailAddress) {
        this.emailAddress = emailAddress;
    }
    
    public String getEditorTheme() {
        return this.editorTheme;
    }
    
    public void setEditorTheme(String editorTheme) {
        this.editorTheme = editorTheme;
    }
    
    public String getLocale() {
        return this.locale;
    }
    
    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    public String getTimeZone() {
        return this.timeZone;
    }
    
    public void setTimeZone(String timeZone) {
        this.timeZone = timeZone;
    }
    
    public Date getDateCreated() {
        if (dateCreated == null) {
            return null;
        } else {
            return (Date)dateCreated.clone();
        }
    }

    public void setDateCreated(final Date date) {
        if (date != null) {
            dateCreated = (Date)date.clone();
        } else {
            dateCreated = null;
        }
    }
    
    public Boolean getDefaultAllowComments() {
        return defaultAllowComments;
    }
    
    public void setDefaultAllowComments(Boolean defaultAllowComments) {
        this.defaultAllowComments = defaultAllowComments;
    }
    
    public int getDefaultCommentDays() {
        return defaultCommentDays;
    }
    
    public void setDefaultCommentDays(int defaultCommentDays) {
        this.defaultCommentDays = defaultCommentDays;
    }
    
    public Boolean getModerateComments() {
        return moderateComments;
    }
    
    public void setModerateComments(Boolean moderateComments) {
        this.moderateComments = moderateComments;
    }
    
    public String getDefaultPlugins() {
        return defaultPlugins;
    }

    public void setDefaultPlugins(String string) {
        defaultPlugins = string;
    }
    
    public int getEntryDisplayCount() {
        return entryDisplayCount;
    }
    
    public void setEntryDisplayCount(int entryDisplayCount) {
        this.entryDisplayCount = entryDisplayCount;
    }
    
    public Boolean getVisible() {
        return this.visible;
    }
    
    public void setVisible(Boolean visible) {
        this.visible = visible;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }
    
    public boolean getEnableMultiLang() {
        return enableMultiLang;
    }
    
    public void setEnableMultiLang(boolean enableMultiLang) {
        this.enableMultiLang = enableMultiLang;
    }
    
    public boolean isEnableMultiLang() {
        return enableMultiLang;
    }
    
    public boolean getShowAllLangs() {
        return showAllLangs;
    }
    
    public void setShowAllLangs(boolean showAllLangs) {
        this.showAllLangs = showAllLangs;
    }
    
    public boolean isShowAllLangs() {
        return showAllLangs;
    }
    
    public String getIconPath() {
        return iconPath;
    }

    public void setIconPath(String iconPath) {
        this.iconPath = iconPath;
    }
    
    public String getAbout() {
        return about;
    }

    public void setAbout(String about) {
        this.about = Utilities.removeHTML(about);
    }

    public String getAnalyticsCode() {
        return analyticsCode;
    }

    public void setAnalyticsCode(String analyticsCode) {
        this.analyticsCode = analyticsCode;
    }

    //------------------------------------------------------- Utility Methods
    
    /**
     * Set bean properties based on other bean.
     */
    public void setData(Weblog other) {
        this.setId(other.getId());
        this.setName(other.getName());
        this.setHandle(other.getHandle());
        this.setTagline(other.getTagline());
        this.setCreatorUserName(other.getCreatorUserName());
        this.setEnableBloggerApi(other.getEnableBloggerApi());
        this.setBloggerCategory(other.getBloggerCategory());
        this.setEditorPage(other.getEditorPage());
        this.setBannedwordslist(other.getBannedwordslist());
        this.setAllowComments(other.getAllowComments());
        this.setEmailComments(other.getEmailComments());
        this.setEmailAddress(other.getEmailAddress());
        this.setEditorTheme(other.getEditorTheme());
        this.setLocale(other.getLocale());
        this.setTimeZone(other.getTimeZone());
        this.setVisible(other.getVisible());
        this.setDateCreated(other.getDateCreated());
        this.setEntryDisplayCount(other.getEntryDisplayCount());
        this.setActive(other.getActive());
        this.setLastModified(other.getLastModified());
        this.setWeblogCategories(other.getWeblogCategories());
    }
    
    /**
     * Parse locale value and instantiate a Locale object,
     * otherwise return default Locale.
     */
    public Locale getLocaleInstance() {
        return I18nUtils.toLocale(getLocale());
    }
    
    /**
     * Return TimeZone instance for value of timeZone,
     * otherwise return system default instance.
     */
    public TimeZone getTimeZoneInstance() {
        if (getTimeZone() == null) {
            this.setTimeZone(TimeZone.getDefault().getID());
        }
        return TimeZone.getTimeZone(getTimeZone());
    }
    
    /**
     * Returns true if user has all permission action specified.
     */
    public boolean hasUserPermission(User user, String action) {
        return hasUserPermissions(user, Collections.singletonList(action));
    }
    
    /**
     * Returns true if user has all permissions actions specified in the weblog.
     */
    public boolean hasUserPermissions(User user, List<String> actions) {
        try {
            // look for user in website's permissions
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            WeblogPermission userPerms = new WeblogPermission(this, user, actions);
            return umgr.checkPermission(userPerms, user);
            
        } catch (WebloggerException ex) {
            // something is going seriously wrong, not much we can do here
            log.error("ERROR checking user permission", ex);
        }
        return false;
    }
    
    /**
     * Returns true if comment moderation is required by website or config.
     */ 
    public boolean getCommentModerationRequired() { 
        return (getModerateComments()
         || WebloggerRuntimeConfig.getBooleanProperty("users.moderation.required"));
    }
    
    /** No-op for bean compatibility */
    public void setCommentModerationRequired(boolean modRequired) {}
    
    /**
     * Get the URL for this weblog.
     */
    public String getURL() {
        return WebloggerFactory.getWeblogger().getUrlStrategy().getWeblogURL(this, null, false);
    }

    /**
     * Get the absolute URL for this weblog.
     */
    public String getAbsoluteURL() {
        return WebloggerFactory.getWeblogger().getUrlStrategy().getWeblogURL(this, null, true);
    }

    //------------------------------------------------------- Category Management

    /**
     * Add a category as a child of this category.
     */
    public void addCategory(WeblogCategory category) {
        if(category == null || category.getName() == null) {
            throw new IllegalArgumentException("Category cannot be null and must have a valid name");
        }

        if(hasCategory(category.getName())) {
            throw new IllegalArgumentException("Duplicate category name '"+category.getName()+"'");
        }

        getWeblogCategories().add(category);
    }

    public List<WeblogCategory> getWeblogCategories() {
        return weblogCategories;
    }

    public void setWeblogCategories(List<WeblogCategory> cats) {
        this.weblogCategories = cats;
    }

    public boolean hasCategory(String name) {
        for (WeblogCategory cat : getWeblogCategories()) {
            if(name.equals(cat.getName())) {
                return true;
            }
        }
        return false;
    }

    //------------------------------------------------------- Bookmark Management

    public List<WeblogBookmarkFolder> getBookmarkFolders() {
        return bookmarkFolders;
    }

    public void setBookmarkFolders(List<WeblogBookmarkFolder> bookmarkFolders) {
        this.bookmarkFolders = bookmarkFolders;
    }

    /**
     * Add a bookmark folder to this weblog.
     */
    public void addBookmarkFolder(WeblogBookmarkFolder folder) {
        if(folder == null || folder.getName() == null) {
            throw new IllegalArgumentException("Folder cannot be null and must have a valid name");
        }

        if(this.hasBookmarkFolder(folder.getName())) {
            throw new IllegalArgumentException("Duplicate folder name '" + folder.getName() + "'");
        }

        getBookmarkFolders().add(folder);
    }

    /**
     * Does this Weblog have a bookmark folder with the specified name?
     */
    public boolean hasBookmarkFolder(String name) {
        for (WeblogBookmarkFolder folder : this.getBookmarkFolders()) {
            if(name.equalsIgnoreCase(folder.getName())) {
                return true;
            }
        }
        return false;
    }

    //------------------------------------------------------- Media File Directory Management

    public List<MediaFileDirectory> getMediaFileDirectories() {
        return mediaFileDirectories;
    }

    public void setMediaFileDirectories(List<MediaFileDirectory> mediaFileDirectories) {
        this.mediaFileDirectories = mediaFileDirectories;
    }

    /**
     * Indicates whether this weblog contains the specified media file directory.
     */
    public boolean hasMediaFileDirectory(String name) {
        for (MediaFileDirectory directory : this.getMediaFileDirectories()) {
            if (directory.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    public MediaFileDirectory getMediaFileDirectory(String name) {
        for (MediaFileDirectory dir : this.getMediaFileDirectories()) {
            if (name.equals(dir.getName())) {
                return dir;
            }
        }
        return null;
    }

}