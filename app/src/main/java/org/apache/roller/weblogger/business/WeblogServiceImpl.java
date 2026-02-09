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

package org.apache.roller.weblogger.business;

import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.weblog.WeblogThemeService;
import org.apache.roller.weblogger.pojos.CommentSearchCriteria;
import org.apache.roller.weblogger.pojos.TagStat;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogBookmarkFolder;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntry.PubStatus;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogEntrySearchCriteria;
import org.apache.roller.weblogger.pojos.WeblogHitCount;
import org.apache.roller.weblogger.pojos.WeblogTheme;


/**
 * Implementation of WeblogService.
 * 
 * This service delegates to various managers (WeblogEntryManager, BookmarkManager,
 * UserManager) to provide high-level business operations on Weblog entities.
 * 
 * @author Apache Roller Team
 */
public class WeblogServiceImpl implements WeblogService {
    
    private static final Log log = LogFactory.getLog(WeblogServiceImpl.class);
    
    private static final int MAX_ENTRIES = 100;
    
    private final WeblogEntryManager entryManager;
    private final BookmarkManager bookmarkManager;
    private final UserManager userManager;
    private final WeblogThemeService themeService;
    
    /**
     * Constructor for dependency injection.
     * 
     * @param entryManager WeblogEntryManager instance.
     * @param bookmarkManager BookmarkManager instance.
     * @param userManager UserManager instance.
     */
    public WeblogServiceImpl(
            WeblogEntryManager entryManager,
            BookmarkManager bookmarkManager,
            UserManager userManager) {
        this.entryManager = entryManager;
        this.bookmarkManager = bookmarkManager;
        this.userManager = userManager;
        this.themeService = new WeblogThemeService();
    }
    
    @Override
    public WeblogTheme getTheme(Weblog weblog) {
        return themeService.getTheme(weblog);
    }
    
    @Override
    public User getCreator(Weblog weblog) throws WebloggerException {
        if (weblog == null || weblog.getCreatorUserName() == null) {
            return null;
        }
        
        try {
            return userManager.getUserByUserName(weblog.getCreatorUserName());
        } catch (WebloggerException e) {
            log.error("ERROR fetching user object for username: " + weblog.getCreatorUserName(), e);
            throw e;
        }
    }
    
    @Override
    public WeblogEntry getWeblogEntry(Weblog weblog, String anchor) 
            throws WebloggerException {
        if (weblog == null || anchor == null) {
            return null;
        }
        
        return entryManager.getWeblogEntryByAnchor(weblog, anchor);
    }
    
    @Override
    public List<WeblogEntry> getRecentEntries(Weblog weblog, int maxEntries) 
            throws WebloggerException {
        
        if (weblog == null) {
            return Collections.emptyList();
        }
        
        // Enforce max limit
        if (maxEntries > MAX_ENTRIES) {
            maxEntries = MAX_ENTRIES;
        }
        if (maxEntries < 1) {
            return Collections.emptyList();
        }
        
        try {
            WeblogEntrySearchCriteria criteria = new WeblogEntrySearchCriteria();
            criteria.setWeblog(weblog);
            criteria.setStatus(PubStatus.PUBLISHED);
            criteria.setMaxResults(maxEntries);
            
            return entryManager.getWeblogEntries(criteria);
            
        } catch (WebloggerException e) {
            log.error("ERROR: getting recent entries for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public List<WeblogEntry> getRecentEntriesWithTags(Weblog weblog, List<String> tags, int maxEntries) 
            throws WebloggerException {
        
        if (weblog == null || tags == null || tags.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Enforce max limit
        if (maxEntries > MAX_ENTRIES) {
            maxEntries = MAX_ENTRIES;
        }
        if (maxEntries < 1) {
            return Collections.emptyList();
        }
        
        try {
            WeblogEntrySearchCriteria criteria = new WeblogEntrySearchCriteria();
            criteria.setWeblog(weblog);
            criteria.setTags(tags);
            criteria.setStatus(PubStatus.PUBLISHED);
            criteria.setMaxResults(maxEntries);
            
            return entryManager.getWeblogEntries(criteria);
            
        } catch (WebloggerException e) {
            log.error("ERROR: getting recent entries with tags for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public List<WeblogEntryComment> getRecentComments(Weblog weblog, int maxComments) 
            throws WebloggerException {
        
        if (weblog == null) {
            return Collections.emptyList();
        }
        
        // Enforce max limit
        if (maxComments > MAX_ENTRIES) {
            maxComments = MAX_ENTRIES;
        }
        if (maxComments < 1) {
            return Collections.emptyList();
        }
        
        try {
            CommentSearchCriteria criteria = new CommentSearchCriteria();
            criteria.setWeblog(weblog);
            criteria.setStatus(WeblogEntryComment.ApprovalStatus.APPROVED);
            criteria.setReverseChrono(true);
            criteria.setMaxResults(maxComments);
            
            return entryManager.getComments(criteria);
            
        } catch (WebloggerException e) {
            log.error("ERROR: getting recent comments for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public WeblogBookmarkFolder getBookmarkFolder(Weblog weblog, String folderName) 
            throws WebloggerException {
        
        if (weblog == null) {
            return null;
        }
        
        try {
            // Handle special cases for default folder
            if (folderName == null || 
                folderName.equals("nil") || 
                folderName.trim().equals("/")) {
                return bookmarkManager.getDefaultFolder(weblog);
            } else {
                return bookmarkManager.getFolder(weblog, folderName);
            }
        } catch (WebloggerException e) {
            log.error("ERROR: fetching folder '" + folderName + "' for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public int getTodaysHits(Weblog weblog) throws WebloggerException {
        if (weblog == null) {
            return 0;
        }
        
        try {
            WeblogHitCount hitCount = entryManager.getHitCountByWeblog(weblog);
            return (hitCount != null) ? hitCount.getDailyHits() : 0;
            
        } catch (WebloggerException e) {
            log.error("Error getting weblog hit count for " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public List<TagStat> getPopularTags(Weblog weblog, int sinceDays, int maxTags) 
            throws WebloggerException {
        
        if (weblog == null) {
            return Collections.emptyList();
        }
        
        Date startDate = null;
        if (sinceDays > 0) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(new Date());
            cal.add(Calendar.DATE, -1 * sinceDays);
            startDate = cal.getTime();
        }
        
        try {
            return entryManager.getPopularTags(weblog, startDate, 0, maxTags);
            
        } catch (WebloggerException e) {
            log.error("ERROR: fetching popular tags for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public long getCommentCount(Weblog weblog) throws WebloggerException {
        if (weblog == null) {
            return 0;
        }
        
        try {
            return entryManager.getCommentCount(weblog);
            
        } catch (WebloggerException e) {
            log.error("Error getting comment count for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public long getEntryCount(Weblog weblog) throws WebloggerException {
        if (weblog == null) {
            return 0;
        }
        
        try {
            return entryManager.getEntryCount(weblog);
            
        } catch (WebloggerException e) {
            log.error("Error getting entry count for weblog " + weblog.getHandle(), e);
            throw e;
        }
    }
    
    @Override
    public void release() {
        // Managers are managed by the Weblogger instance
        // This method is here for API consistency and future use
    }
}   