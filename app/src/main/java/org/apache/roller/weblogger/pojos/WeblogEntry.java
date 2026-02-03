/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.util.DateUtil;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.util.UUIDGenerator;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.business.plugins.entry.WeblogEntryPlugin;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.util.HTMLSanitizer;
import org.apache.roller.weblogger.util.I18nMessages;
import org.apache.roller.weblogger.util.Utilities;

/**
 * Represents a Weblog Entry.
 */
public class WeblogEntry implements Serializable {
    private static final Log mLogger = LogFactory.getFactory().getInstance(WeblogEntry.class);
    
    public static final long serialVersionUID = 2341505386843044125L;

    public enum PubStatus {DRAFT, PUBLISHED, PENDING, SCHEDULED}

    private static final char TITLE_SEPARATOR =
        WebloggerConfig.getBooleanProperty("weblogentry.title.useUnderscoreSeparator") ? '_' : '-';

    // Simple properies
    private String    id            = UUIDGenerator.generateUUID();
    private String    title         = null;
    private String    link          = null;
    private String    summary       = null;
    private String    text          = null;
    private String    contentType   = null;
    private String    contentSrc    = null;
    private String    anchor        = null;
    private Timestamp pubTime       = null;
    private Timestamp updateTime    = null;
    private String    plugins       = null;
    private Boolean   allowComments = Boolean.TRUE;
    private Integer   commentDays   = 7;
    private Boolean   rightToLeft   = Boolean.FALSE;
    private Boolean   pinnedToMain  = Boolean.FALSE;
    private PubStatus status        = PubStatus.DRAFT;
    private String    locale        = null;
    private String    creatorUserName = null;      
    private String    searchDescription = null;

    // set to true when switching between pending/draft/scheduled and published
    // either the aggregate table needs the entry's tags added (for published)
    // or subtracted (anything else)
    private Boolean   refreshAggregates = Boolean.FALSE;

    // Associated objects
    private Weblog        website  = null;
    private WeblogCategory category = null;
    
    // Collection of name/value entry attributes
    private Set<WeblogEntryAttribute> attSet = new TreeSet<>();
    
    private Set<WeblogEntryTag> tagSet = new HashSet<>();
    private Set<WeblogEntryTag> removedTags = new HashSet<>();
    private Set<WeblogEntryTag> addedTags = new HashSet<>();
    
    //----------------------------------------------------------- Construction
    
    public WeblogEntry() {
    }
    
    public WeblogEntry(
            String id,
            WeblogCategory category,
            Weblog website,
            User creator,
            String title,
            String link,
            String text,
            String anchor,
            Timestamp pubTime,
            Timestamp updateTime,
            PubStatus status) {
        //this.id = id;
        this.category = category;
        this.website = website;
        this.creatorUserName = creator.getUserName();
        this.title = title;
        this.link = link;
        this.text = text;
        this.anchor = anchor;
        this.pubTime = pubTime;
        this.updateTime = updateTime;
        this.status = status;
    }
    
    public WeblogEntry(WeblogEntry otherData) {
        this.setData(otherData);
    }
    
    //---------------------------------------------------------- Initializaion
    
    /**
     * Set bean properties based on other bean.
     */
    public void setData(WeblogEntry other) {
        
        this.setId(other.getId());
        this.setCategory(other.getCategory());
        this.setWebsite(other.getWebsite());
        this.setCreatorUserName(other.getCreatorUserName());
        this.setTitle(other.getTitle());
        this.setLink(other.getLink());
        this.setText(other.getText());
        this.setSummary(other.getSummary());
        this.setSearchDescription(other.getSearchDescription());
        this.setAnchor(other.getAnchor());
        this.setPubTime(other.getPubTime());
        this.setUpdateTime(other.getUpdateTime());
        this.setStatus(other.getStatus());
        this.setPlugins(other.getPlugins());
        this.setAllowComments(other.getAllowComments());
        this.setCommentDays(other.getCommentDays());
        this.setRightToLeft(other.getRightToLeft());
        this.setPinnedToMain(other.getPinnedToMain());
        this.setLocale(other.getLocale());
    }
    
    //------------------------------------------------------- Good citizenship

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append("{");
        buf.append(getId());
        buf.append(", ").append(this.getAnchor());
        buf.append(", ").append(this.getTitle());
        buf.append(", ").append(this.getPubTime());
        buf.append("}");
        return buf.toString();
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof WeblogEntry)) {
            return false;
        }
        WeblogEntry o = (WeblogEntry)other;
        return new EqualsBuilder()
            .append(getAnchor(), o.getAnchor()) 
            .append(getWebsite(), o.getWebsite()) 
            .isEquals();
    }
    
    @Override
    public int hashCode() { 
        return new HashCodeBuilder()
            .append(getAnchor())
            .append(getWebsite())
            .toHashCode();
    }
    
   //------------------------------------------------------ Simple properties
    
    public String getId() {
        return this.id;
    }
    
    public void setId(String id) {
        // Form bean workaround: empty string is never a valid id
        if (id != null && id.isBlank()) {
            return;
        }
        this.id = id;
    }
    
    public WeblogCategory getCategory() {
        return this.category;
    }
    
    public void setCategory(WeblogCategory category) {
        this.category = category;
    }
       
    /**
     * Return collection of WeblogCategory objects of this entry.
     * Added for symmetry with PlanetEntryData object.
     */
    public List<WeblogCategory> getCategories() {
        return List.of(getCategory());
    }
    
    public Weblog getWebsite() {
        return this.website;
    }
    
    public void setWebsite(Weblog website) {
        this.website = website;
    }
    
    public User getCreator() {
        try {
            return WebloggerFactory.getWeblogger().getUserManager().getUserByUserName(getCreatorUserName());
        } catch (Exception e) {
            mLogger.error("ERROR fetching user object for username: " + getCreatorUserName(), e);
        }
        return null;
    }   
    
    public String getCreatorUserName() {
        return creatorUserName;
    }

    public void setCreatorUserName(String creatorUserName) {
        this.creatorUserName = creatorUserName;
    }   
    
    public String getTitle() {
        return this.title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Get summary for weblog entry (maps to RSS description and Atom summary).
     */
    public String getSummary() {
        return summary;
    }
    
    /**
     * Set summary for weblog entry (maps to RSS description and Atom summary).
     */
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    /**
     * Get search description for weblog entry.
     */
    public String getSearchDescription() {
        return searchDescription;
    }
    
    /**
     * Set search description for weblog entry
     */
    public void setSearchDescription(String searchDescription) {
        this.searchDescription = searchDescription;
    }

    /**
     * Get content text for weblog entry (maps to RSS content:encoded and Atom content).
     */
    public String getText() {
        return this.text;
    }
    
    /**
     * Set content text for weblog entry (maps to RSS content:encoded and Atom content).
     */
    public void setText(String text) {
        this.text = text;
    }
    
    /**
     * Get content type (text, html, xhtml or a MIME content type)
     */
    public String getContentType() {
        return contentType;
    }
    
    /**
     * Set content type (text, html, xhtml or a MIME content type)
     */
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    /**
     * Get URL for out-of-line content.
     */
    public String getContentSrc() {
        return contentSrc;
    }
    
    /**
     * Set URL for out-of-line content.
     */
    public void setContentSrc(String contentSrc) {
        this.contentSrc = contentSrc;
    }
    
    public String getAnchor() {
        return this.anchor;
    }
    
    public void setAnchor(String anchor) {
        this.anchor = anchor;
    }
    
    //-------------------------------------------------------------------------

    public Set<WeblogEntryAttribute> getEntryAttributes() {
        return attSet;
    }

    public void setEntryAttributes(Set<WeblogEntryAttribute> atts) {
        this.attSet = atts;
    }
    
    public String findEntryAttribute(String name) {
        if (getEntryAttributes() == null) {
            return null;
        }
        
        for (WeblogEntryAttribute att : getEntryAttributes()) {
            if (name.equals(att.getName())) {
                return att.getValue();
            }
        }
        return null;
    }
        
    public void putEntryAttribute(String name, String value) throws Exception {
        WeblogEntryAttribute att = findAttributeByName(name);
        
        if (att == null) {
            att = createNewAttribute(name, value);
            getEntryAttributes().add(att);
        } else {
            att.setValue(value);
        }
    }

    private WeblogEntryAttribute findAttributeByName(String name) {
        for (WeblogEntryAttribute o : getEntryAttributes()) {
            if (name.equals(o.getName())) {
                return o;
            }
        }
        return null;
    }

    private WeblogEntryAttribute createNewAttribute(String name, String value) {
        WeblogEntryAttribute att = new WeblogEntryAttribute();
        att.setEntry(this);
        att.setName(name);
        att.setValue(value);
        return att;
    }
    
    //-------------------------------------------------------------------------
    
    /**
     * <p>Publish time is the time that an entry is to be (or was) made available
     * for viewing by newsfeed readers and visitors to the Roller site.</p>
     *
     * <p>Roller stores time using the timeZone of the server itself. When
     * times are displayed  in a user's weblog they must be translated
     * to the user's timeZone.</p>
     *
     * <p>NOTE: Times are stored using the SQL TIMESTAMP datatype, which on
     * MySQL has only a one-second resolution.</p>
     */
    public Timestamp getPubTime() {
        return this.pubTime;
    }
    
    public void setPubTime(Timestamp pubTime) {
        this.pubTime = pubTime;
    }
    
    /**
     * <p>Update time is the last time that an weblog entry was saved in the
     * Roller weblog editor or via web services API (XML-RPC or Atom).</p>
     *
     * <p>Roller stores time using the timeZone of the server itself. When
     * times are displayed  in a user's weblog they must be translated
     * to the user's timeZone.</p>
     *
     * <p>NOTE: Times are stored using the SQL TIMESTAMP datatype, which on
     * MySQL has only a one-second resolution.</p>
     */
    public Timestamp getUpdateTime() {
        return this.updateTime;
    }
    
    public void setUpdateTime(Timestamp updateTime) {
        this.updateTime = updateTime;
    }
    
    public PubStatus getStatus() {
        return this.status;
    }
    
    public void setStatus(PubStatus status) {
        this.status = status;
    }
    
    /**
     * Some weblog entries are about one specific link.
     * @return Returns the link.
     */
    public String getLink() {
        return link;
    }
    
    /**
     * @param link The link to set.
     */
    public void setLink(String link) {
        this.link = link;
    }
    
    /**
     * Comma-delimited list of this entry's Plugins.
     */
    public String getPlugins() {
        return plugins;
    }
    
    public void setPlugins(String string) {
        plugins = string;
    }

    /**
     * True if comments are allowed on this weblog entry.
     */
    public Boolean getAllowComments() {
        return allowComments;
    }
    /**
     * True if comments are allowed on this weblog entry.
     */
    public void setAllowComments(Boolean allowComments) {
        this.allowComments = allowComments;
    }
    
    /**
     * Number of days after pubTime that comments should be allowed, or 0 for no limit.
     */
    public Integer getCommentDays() {
        return commentDays;
    }
    /**
     * Number of days after pubTime that comments should be allowed, or 0 for no limit.
     */
    public void setCommentDays(Integer commentDays) {
        this.commentDays = commentDays;
    }
    
    /**
     * True if this entry should be rendered right to left.
     */
    public Boolean getRightToLeft() {
        return rightToLeft;
    }
    /**
     * True if this entry should be rendered right to left.
     */
    public void setRightToLeft(Boolean rightToLeft) {
        this.rightToLeft = rightToLeft;
    }
    
    /**
     * True if story should be pinned to the top of the Roller site main blog.
     * @return Returns the pinned.
     */
    public Boolean getPinnedToMain() {
        return pinnedToMain;
    }
    /**
     * True if story should be pinned to the top of the Roller site main blog.
     * @param pinnedToMain The pinned to set.
     */
    public void setPinnedToMain(Boolean pinnedToMain) {
        this.pinnedToMain = pinnedToMain;
    }

    /**
     * The locale string that defines the i18n approach for this entry.
     */
    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
    
    public Set<WeblogEntryTag> getTags() {
         return tagSet;
    }

    @SuppressWarnings("unused")
    public void setTags(Set<WeblogEntryTag> tagSet) throws WebloggerException {
         this.tagSet = tagSet;
         this.removedTags = new HashSet<>();
         this.addedTags = new HashSet<>();
    }
     
    /**
     * Roller lowercases all tags based on locale because there's not a 1:1 mapping
     * between uppercase/lowercase characters across all languages.  
     * @param name
     * @throws WebloggerException
     */
    public void addTag(String name) throws WebloggerException {
        Locale localeObject = getLocaleForNormalization();
        name = Utilities.normalizeTag(name, localeObject);
        
        if (name.length() == 0) {
            return;
        }
        
        if (tagAlreadyExists(name)) {
            return;
        }

        WeblogEntryTag tag = createTag(name);
        tagSet.add(tag);
        addedTags.add(tag);
    }

    private Locale getLocaleForNormalization() {
        return getWebsite() != null ? getWebsite().getLocaleInstance() : Locale.getDefault();
    }

    private boolean tagAlreadyExists(String name) {
        for (WeblogEntryTag tag : getTags()) {
            if (tag.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    private WeblogEntryTag createTag(String name) {
        WeblogEntryTag tag = new WeblogEntryTag();
        tag.setName(name);
        tag.setCreatorUserName(getCreatorUserName());
        tag.setWeblog(getWebsite());
        tag.setWeblogEntry(this);
        tag.setTime(getUpdateTime());
        return tag;
    }

    public Set<WeblogEntryTag> getAddedTags() {
        return addedTags;
    }
    
    public Set<WeblogEntryTag> getRemovedTags() {
        return removedTags;
    }

    public String getTagsAsString() {
        StringBuilder sb = new StringBuilder();
        // Sort by name
        Set<WeblogEntryTag> tmp = new TreeSet<>(new WeblogEntryTagComparator());
        tmp.addAll(getTags());
        
        for (WeblogEntryTag entryTag : tmp) {
            sb.append(entryTag.getName()).append(" ");
        }
        
        if (sb.length() > 0) {
            sb.deleteCharAt(sb.length() - 1);
        }

        return sb.toString();
    }

    public void setTagsAsString(String tags) throws WebloggerException {
        if (StringUtils.isEmpty(tags)) {
            clearAllTags();
            return;
        }

        List<String> updatedTags = Utilities.splitStringAsTags(tags);
        Set<String> newTags = normalizeTagList(updatedTags);

        removeObsoleteTags(newTags);
        addNewTags(newTags);
    }

    private void clearAllTags() {
        removedTags.addAll(tagSet);
        tagSet.clear();
    }

    private Set<String> normalizeTagList(List<String> updatedTags) {
        Set<String> newTags = new HashSet<>(updatedTags.size());
        Locale localeObject = getLocaleForNormalization();

        for (String name : updatedTags) {
            newTags.add(Utilities.normalizeTag(name, localeObject));
        }
        return newTags;
    }

    private void removeObsoleteTags(Set<String> newTags) {
        for (Iterator<WeblogEntryTag> it = tagSet.iterator(); it.hasNext();) {
            WeblogEntryTag tag = it.next();
            if (!newTags.contains(tag.getName())) {
                removedTags.add(tag);
                it.remove();
            } else {
                // already in persisted set, therefore isn't new
                newTags.remove(tag.getName());
            }
        }
    }

    private void addNewTags(Set<String> newTags) throws WebloggerException {
        for (String newTag : newTags) {
            addTag(newTag);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
     * True if comments are still allowed on this entry considering the
     * allowComments and commentDays fields as well as the website and 
     * site-wide configs.
     */
    public boolean getCommentsStillAllowed() {
        if (!areSiteCommentsEnabled()) {
            return false;
        }
        if (!areWeblogCommentsEnabled()) {
            return false;
        }
        if (!areEntryCommentsEnabled()) {
            return false;
        }
        return isWithinCommentPeriod();
    }

    private boolean areSiteCommentsEnabled() {
        return WebloggerRuntimeConfig.getBooleanProperty("users.comments.enabled");
    }

    private boolean areWeblogCommentsEnabled() {
        return getWebsite().getAllowComments() == null || getWebsite().getAllowComments();
    }

    private boolean areEntryCommentsEnabled() {
        return getAllowComments() == null || getAllowComments();
    }

    private boolean isWithinCommentPeriod() {
        if (getCommentDays() == null || getCommentDays() == 0) {
            return true; // No time limit
        }

        Date effectivePubTime = getEffectivePubTime();
        Date expiryDate = calculateCommentExpiryDate(effectivePubTime);
        
        return new Date().before(expiryDate);
    }

    private Date getEffectivePubTime() {
        // Use pubtime for calculating when comments expire, but
        // if pubtime isn't set (like for drafts) then use updatetime
        Date inPubTime = getPubTime();
        return (inPubTime != null) ? inPubTime : getUpdateTime();
    }

    private Date calculateCommentExpiryDate(Date fromDate) {
        Calendar expireCal = Calendar.getInstance(getWebsite().getLocaleInstance());
        expireCal.setTime(fromDate);
        expireCal.add(Calendar.DATE, getCommentDays());
        return expireCal.getTime();
    }

    public void setCommentsStillAllowed(boolean ignored) {
        // no-op
    }
    
    
    //------------------------------------------------------------------------
    
    /**
     * Format the publish time of this weblog entry using the specified pattern.
     * See java.text.SimpleDateFormat for more information on this format.
     *
     * @see java.text.SimpleDateFormat
     * @return Publish time formatted according to pattern.
     */
    public String formatPubTime(String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern,
                    this.getWebsite().getLocaleInstance());
            
            return format.format(getPubTime());
        } catch (RuntimeException e) {
            mLogger.error("Unexpected exception", e);
        }
        
        return "ERROR: formatting date";
    }
    
    //------------------------------------------------------------------------
    
    /**
     * Format the update time of this weblog entry using the specified pattern.
     * See java.text.SimpleDateFormat for more information on this format.
     *
     * @see java.text.SimpleDateFormat
     * @return Update time formatted according to pattern.
     */
    public String formatUpdateTime(String pattern) {
        try {
            SimpleDateFormat format = new SimpleDateFormat(pattern);
            
            return format.format(getUpdateTime());
        } catch (RuntimeException e) {
            mLogger.error("Unexpected exception", e);
        }
        
        return "ERROR: formatting date";
    }
    
    //------------------------------------------------------------------------
    
    public List<WeblogEntryComment> getComments() {
        return getComments(true, true);
    }
    
    /**
     * TODO: why is this method exposed to users with ability to get spam/non-approved comments?
     */
    @Deprecated
    public List<WeblogEntryComment> getComments(boolean ignoreSpam, boolean approvedOnly) {
        try {
            WeblogEntryManager wmgr = WebloggerFactory.getWeblogger().getWeblogEntryManager();

            CommentSearchCriteria csc = new CommentSearchCriteria();
            csc.setWeblog(getWebsite());
            csc.setEntry(this);
            csc.setStatus(approvedOnly ? WeblogEntryComment.ApprovalStatus.APPROVED : null);
            return wmgr.getComments(csc);
        } catch (WebloggerException alreadyLogged) {}
        
        return Collections.emptyList();
    }
    
    public int getCommentCount() {
        return getComments().size();
    }
    
    //------------------------------------------------------------------------
        
    /**
     * Returns absolute entry permalink.
     */
    public String getPermalink() {
        return WebloggerFactory.getWeblogger().getUrlStrategy().getWeblogEntryURL(getWebsite(), null, getAnchor(), true);
    }
    
    /**
     * Returns entry permalink, relative to Roller context.
     * @deprecated Use getPermalink() instead.
     */
    @Deprecated
    public String getPermaLink() {
        String lAnchor = URLEncoder.encode(getAnchor(), StandardCharsets.UTF_8);
        return "/" + getWebsite().getHandle() + "/entry/" + lAnchor;
    }
    
    /**
     * Get relative URL to comments page.
     * @deprecated Use commentLink() instead
     */
    @Deprecated
    public String getCommentsLink() {
        return getPermaLink() + "#comments";
    }
    
    /**
     * Return the Title of this post, or the first 255 characters of the
     * entry's text.
     *
     * @return String
     */
    public String getDisplayTitle() {
        if (StringUtils.isBlank(getTitle())) {
            return StringUtils.left(Utilities.removeHTML(getText()), RollerConstants.TEXTWIDTH_255);
        }
        return Utilities.removeHTML(getTitle());
    }
    
    /**
     * Return RSS 09x style description (escaped HTML version of entry text)
     */
    public String getRss09xDescription() {
        return getRss09xDescription(-1);
    }
    
    /**
     * Return RSS 09x style description (escaped HTML version of entry text)
     */
    public String getRss09xDescription(int maxLength) {
        String ret = StringEscapeUtils.escapeHtml3(getText());
        if (maxLength != -1 && ret.length() > maxLength) {
            ret = ret.substring(0, maxLength - 3) + "...";
        }
        return ret;
    }
    
    /** Create anchor for weblog entry, based on title or text */
    protected String createAnchor() throws WebloggerException {
        return WebloggerFactory.getWeblogger().getWeblogEntryManager().createAnchor(this);
    }
    
    /** Create anchor for weblog entry, based on title or text */
    public String createAnchorBase() {
        String base = extractAnchorFromTitleOrText();
        
        if (StringUtils.isNotEmpty(base)) {
            base = buildAnchorFromWords(base);
        } else {
            // No title or text, use date in YYYYMMDD format
            base = DateUtil.format8chars(getPubTime());
        }
        
        return base;
    }

    private String extractAnchorFromTitleOrText() {
        // Try title first (minus non-alphanumeric characters)
        if (StringUtils.isNotEmpty(getTitle())) {
            return Utilities.replaceNonAlphanumeric(getTitle(), ' ').trim();
        }
        
        // Fall back to text (minus non-alphanumerics)
        if (StringUtils.isNotEmpty(getText())) {
            return Utilities.replaceNonAlphanumeric(getText(), ' ').trim();
        }
        
        return null;
    }

    private String buildAnchorFromWords(String base) {
        // Use only the first 4 words
        StringTokenizer toker = new StringTokenizer(base);
        StringBuilder result = new StringBuilder();
        int count = 0;
        
        while (toker.hasMoreTokens() && count < 5) {
            String word = toker.nextToken().toLowerCase();
            if (result.length() > 0) {
                result.append(TITLE_SEPARATOR);
            }
            result.append(word);
            count++;
        }
        
        return result.toString();
    }
    
    /**
     * A no-op. TODO: fix formbean generation so this is not needed.
     */
    public void setPermalink(String string) {}
    
    /**
     * A no-op. TODO: fix formbean generation so this is not needed.
     */
    public void setPermaLink(String string) {}
    
    /**
     * A no-op.
     * TODO: fix formbean generation so this is not needed.
     * @param string
     */
    public void setDisplayTitle(String string) {
    }
    
    /**
     * A no-op.
     * TODO: fix formbean generation so this is not needed.
     * @param string
     */
    public void setRss09xDescription(String string) {
    }
    
    
    /**
     * Convenience method to transform mPlugins to a List
     * @return
     */
    public List<String> getPluginsList() {
        if (getPlugins() != null) {
            return Arrays.asList(StringUtils.split(getPlugins(), ","));
        }
        return Collections.emptyList();
    }

    /** Convenience method for checking status */
    public boolean isDraft() {
        return getStatus().equals(PubStatus.DRAFT);
    }

    /** Convenience method for checking status */
    public boolean isPending() {
        return getStatus().equals(PubStatus.PENDING);
    }

    /** Convenience method for checking status */
    public boolean isPublished() {
        return getStatus().equals(PubStatus.PUBLISHED);
    }

    /**
     * Get entry text, transformed by plugins enabled for entry.
     */
    public String getTransformedText() {
        return render(getText());
    }

    /**
     * Get entry summary, transformed by plugins enabled for entry.
     */
    public String getTransformedSummary() {
        return render(getSummary());
    }

    /**
     * Determine if the specified user has permissions to edit this entry.
     */
    public boolean hasWritePermissions(User user) throws WebloggerException {
        if (userHasGlobalAdminPermission(user)) {
            return true;
        }
        
        WeblogPermission perm = getUserWeblogPermission(user);
        return userCanEditEntry(perm);
    }

    private boolean userHasGlobalAdminPermission(User user) throws WebloggerException {
        GlobalPermission adminPerm = new GlobalPermission(
                Collections.singletonList(GlobalPermission.ADMIN));
        return WebloggerFactory.getWeblogger().getUserManager()
                .checkPermission(adminPerm, user);
    }

    private WeblogPermission getUserWeblogPermission(User user) throws WebloggerException {
        try {
            UserManager umgr = WebloggerFactory.getWeblogger().getUserManager();
            return umgr.getWeblogPermission(getWebsite(), user);
        } catch (WebloggerException ex) {
            mLogger.error("ERROR retrieving user's permission", ex);
            throw ex;
        }
    }

    private boolean userCanEditEntry(WeblogPermission perm) {
        boolean author = perm.hasAction(WeblogPermission.POST) 
                || perm.hasAction(WeblogPermission.ADMIN);
        boolean limited = !author && perm.hasAction(WeblogPermission.EDIT_DRAFT);
        
        return author || (limited && (status == PubStatus.DRAFT || status == PubStatus.PENDING));
    }
    
    /**
     * Transform string based on plugins enabled for this weblog entry.
     */
    private String render(String str) {
        if (str == null) {
            return HTMLSanitizer.conditionallySanitize(null);
        }

        String ret = str;
        mLogger.debug("Applying page plugins to string");
        
        Map<String, WeblogEntryPlugin> inPlugins = getWebsite().getInitializedPlugins();
        if (inPlugins != null) {
            ret = applyPlugins(ret, inPlugins);
        }
        
        return HTMLSanitizer.conditionallySanitize(ret);
    }

    private String applyPlugins(String content, Map<String, WeblogEntryPlugin> inPlugins) {
        List<String> entryPlugins = getPluginsList();
        
        // If no Entry plugins, skip processing
        if (entryPlugins == null || entryPlugins.isEmpty()) {
            return content;
        }

        String result = content;
        for (Map.Entry<String, WeblogEntryPlugin> entry : inPlugins.entrySet()) {
            if (entryPlugins.contains(entry.getKey())) {
                result = applyPlugin(result, entry.getValue());
            }
        }
        return result;
    }

    private String applyPlugin(String content, WeblogEntryPlugin plugin) {
        try {
            return plugin.render(this, content);
        } catch (Exception e) {
            mLogger.error("ERROR from plugin: " + plugin.getName(), e);
            return content;
        }
    }
    
    
    /**
     * Get the right transformed display content depending on the situation.
     *
     * If the readMoreLink is specified then we assume the caller wants to
     * prefer summary over content and we include a "Read More" link at the
     * end of the summary if it exists.  Otherwise, if the readMoreLink is
     * empty or null then we assume the caller prefers content over summary.
     */
    public String displayContent(String readMoreLink) {
        if (isPermalinkContext(readMoreLink)) {
            return getPermalinkContent();
        }
        return getListingContent(readMoreLink);
    }

    private boolean isPermalinkContext(String readMoreLink) {
        return readMoreLink == null || readMoreLink.isBlank() || "nil".equals(readMoreLink);
    }

    private String getPermalinkContent() {
        // Permalink context: prefer text over summary
        if (StringUtils.isNotEmpty(getText())) {
            return getTransformedText();
        }
        return getTransformedSummary();
    }

    private String getListingContent(String readMoreLink) {
        // Listing context: prefer summary over text, add "read more" if needed
        if (StringUtils.isNotEmpty(getSummary())) {
            return getSummaryWithReadMore(readMoreLink);
        }
        return getTransformedText();
    }

    private String getSummaryWithReadMore(String readMoreLink) {
        String displayContent = getTransformedSummary();
        
        if (StringUtils.isNotEmpty(getText())) {
            String readMore = buildReadMoreLink(readMoreLink);
            displayContent += readMore;
        }
        
        return displayContent;
    }

    private String buildReadMoreLink(String readMoreLink) {
        List<String> args = List.of(readMoreLink);
        return I18nMessages.getMessages(getWebsite().getLocaleInstance())
                .getString("macro.weblog.readMoreLink", args);
    }
    
    
    /**
     * Get the right transformed display content.
     */
    public String getDisplayContent() { 
        return displayContent(null);
    }

    public Boolean getRefreshAggregates() {
        return refreshAggregates;
    }

    public void setRefreshAggregates(Boolean refreshAggregates) {
        this.refreshAggregates = refreshAggregates;
    }

}