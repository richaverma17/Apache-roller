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
 * limitations under the License.
 */

package org.apache.roller.weblogger.business;

import com.google.inject.Inject;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.business.BookmarkManager;
import org.apache.roller.weblogger.business.MediaFileManager;
import org.apache.roller.weblogger.business.FileContentManager;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.WeblogEntryManager;
/**
 * Facade grouping content-related managers:
 *   - WeblogManager
 *   - WeblogEntryManager
 *   - BookmarkManager
 *   - MediaFileManager
 *   - FileContentManager
 */
@com.google.inject.Singleton
public class ContentManagerFacade {

    private static final Log log = LogFactory.getLog(ContentManagerFacade.class);

    private final BookmarkManager     bookmarkManager;
    private final MediaFileManager    mediaFileManager;
    private final FileContentManager  fileContentManager;
    private final WeblogManager       weblogManager;
    private final WeblogEntryManager  weblogEntryManager;

    @Inject
    public ContentManagerFacade(
            BookmarkManager     bookmarkManager,
            MediaFileManager    mediaFileManager,
            FileContentManager  fileContentManager,
            WeblogManager       weblogManager,
            WeblogEntryManager  weblogEntryManager) {

        this.bookmarkManager     = bookmarkManager;
        this.mediaFileManager    = mediaFileManager;
        this.fileContentManager  = fileContentManager;
        this.weblogManager       = weblogManager;
        this.weblogEntryManager  = weblogEntryManager;
    }

    // Getters

    public BookmarkManager     getBookmarkManager()     { return bookmarkManager; }
    public MediaFileManager    getMediaFileManager()    { return mediaFileManager; }
    public FileContentManager  getFileContentManager()  { return fileContentManager; }
    public WeblogManager       getWeblogManager()       { return weblogManager; }
    public WeblogEntryManager  getWeblogEntryManager()  { return weblogEntryManager; }

    // Lifecycle methods

    public void initialize() throws InitializationException {
        // In original code only MediaFileManager had explicit initialize()
        try {
            mediaFileManager.initialize();
        } catch (Exception e) {
            throw new InitializationException("Failed to initialize media file manager", e);
        }
    }

    public void release() {
        try {
            bookmarkManager.release();
            mediaFileManager.release();
            fileContentManager.release();
            weblogManager.release();
            weblogEntryManager.release();
        } catch (Exception e) {
            log.error("Error releasing content-related managers", e);
        }
    }

    public void shutdown() {
        // No specific shutdown logic was present in original code for these components
    }
}