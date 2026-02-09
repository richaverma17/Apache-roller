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

import java.util.Date;
import java.util.List;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.pojos.TagStat;
import org.apache.roller.weblogger.pojos.User;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogBookmarkFolder;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.pojos.WeblogEntryComment;
import org.apache.roller.weblogger.pojos.WeblogTheme;


/**
 * Service facade for Weblog-related operations.
 * 
 * This service coordinates calls across multiple managers (WeblogEntryManager,
 * BookmarkManager, UserManager, etc.) to provide high-level business operations
 * related to Weblog entities.
 * 
 * This service was created to break the cyclic dependency between the Weblog
 * domain entity and the service layer. Previously, Weblog contained convenience
 * methods that called back into WebloggerFactory and various managers, creating
 * a circular dependency. Now Weblog is a pure POJO and all business logic is
 * handled through this service.
 * 
 * Usage Example:
 * <pre>
 * WeblogService service = WebloggerFactory.getWeblogger().getWeblogService();
 * 
 * // Get recent entries
 * List&lt;WeblogEntry&gt; entries = service.getRecentEntries(weblog, 10);
 * 
 * // Get popular tags
 * List&lt;TagStat&gt; tags = service.getPopularTags(weblog, 30, 20);
 * 
 * // Get comment count
 * long count = service.getCommentCount(weblog);
 * </pre>
 * 
 * @author Apache Roller Team
 */
public interface WeblogService {
    
    /**
     * Get the theme object in use by this weblog.
     * 
     * @param weblog The weblog to get the theme for.
     * @return WeblogTheme object or null if no theme is set.
     */
    WeblogTheme getTheme(Weblog weblog);
    
    /**
     * Get the User who created this weblog.
     * 
     * @param weblog The weblog to get the creator for.
     * @return User object of the creator, or null if not found.
     * @throws WebloggerException If there is a problem fetching the user.
     */
    User getCreator(Weblog weblog) throws WebloggerException;
    
    /**
     * Get a weblog entry by its anchor (permalink).
     * 
     * @param weblog The weblog to search in.
     * @param anchor The anchor/permalink of the entry.
     * @return WeblogEntry object or null if not found.
     * @throws WebloggerException If there is a problem fetching the entry.
     */
    WeblogEntry getWeblogEntry(Weblog weblog, String anchor) throws WebloggerException;
    
    /**
     * Get recent published weblog entries.
     * 
     * @param weblog The weblog to get entries from.
     * @param maxEntries Maximum number of entries to return (1-100).
     * @return List of WeblogEntry objects, empty list if none found.
     * @throws WebloggerException If there is a problem fetching entries.
     */
    List<WeblogEntry> getRecentEntries(Weblog weblog, int maxEntries) 
            throws WebloggerException;
    
    /**
     * Get recent published weblog entries with specific tags.
     * 
     * @param weblog The weblog to get entries from.
     * @param tags List of tags to filter by.
     * @param maxEntries Maximum number of entries to return (1-100).
     * @return List of WeblogEntry objects, empty list if none found.
     * @throws WebloggerException If there is a problem fetching entries.
     */
    List<WeblogEntry> getRecentEntriesWithTags(Weblog weblog, List<String> tags, int maxEntries) 
            throws WebloggerException;
    
    /**
     * Get recent approved and non-spam comments for a weblog.
     * 
     * @param weblog The weblog to get comments from.
     * @param maxComments Maximum number of comments to return (1-100).
     * @return List of WeblogEntryComment objects, empty list if none found.
     * @throws WebloggerException If there is a problem fetching comments.
     */
    List<WeblogEntryComment> getRecentComments(Weblog weblog, int maxComments) 
            throws WebloggerException;
    
    /**
     * Get bookmark folder by name.
     * 
     * @param weblog The weblog to search in.
     * @param folderName Name or path of the bookmark folder (null for default folder).
     * @return WeblogBookmarkFolder object or null if not found.
     * @throws WebloggerException If there is a problem fetching the folder.
     */
    WeblogBookmarkFolder getBookmarkFolder(Weblog weblog, String folderName) 
            throws WebloggerException;
    
    /**
     * Get the number of hits counted today for a weblog.
     * 
     * @param weblog The weblog to get hit count for.
     * @return Number of hits today, 0 if none or if hit counting is disabled.
     * @throws WebloggerException If there is a problem fetching hit count.
     */
    int getTodaysHits(Weblog weblog) throws WebloggerException;
    
    /**
     * Get popular tags for a weblog.
     * 
     * @param weblog The weblog to get tags from.
     * @param sinceDays Number of days into the past to consider (-1 for all time).
     * @param maxTags Maximum number of tags to return.
     * @return List of TagStat objects ordered by popularity, empty list if none found.
     * @throws WebloggerException If there is a problem fetching tags.
     */
    List<TagStat> getPopularTags(Weblog weblog, int sinceDays, int maxTags) 
            throws WebloggerException;
    
    /**
     * Get the total number of comments for a weblog.
     * 
     * @param weblog The weblog to get comment count for.
     * @return Total number of comments.
     * @throws WebloggerException If there is a problem fetching the count.
     */
    long getCommentCount(Weblog weblog) throws WebloggerException;
    
    /**
     * Get the total number of entries for a weblog.
     * 
     * @param weblog The weblog to get entry count for.
     * @return Total number of entries.
     * @throws WebloggerException If there is a problem fetching the count.
     */
    long getEntryCount(Weblog weblog) throws WebloggerException;
    
    /**
     * Release all resources associated with this service.
     * 
     * This method is called when the Weblogger instance is being shut down.
     * Individual manager resources are managed by the Weblogger instance,
     * so this method typically doesn't need to do anything.
     */
    void release();
}