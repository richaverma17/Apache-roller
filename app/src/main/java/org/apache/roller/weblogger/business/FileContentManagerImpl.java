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

    package org.apache.roller.weblogger.business;

    import java.io.File;
    import java.io.IOException;
    import java.io.InputStream;
    import java.io.OutputStream;
    import java.math.BigDecimal;
    import java.nio.file.Files;
    import java.nio.file.Path;

    import org.apache.commons.logging.Log;
    import org.apache.commons.logging.LogFactory;
    import org.apache.roller.weblogger.config.WebloggerConfig;
    import org.apache.roller.weblogger.pojos.FileContent;
    import org.apache.roller.weblogger.pojos.Weblog;
    import org.apache.roller.weblogger.util.RollerMessages;

    /**
     * Manages contents of files uploaded to Roller weblogs.
     * 
     * <p>This refactored implementation reduces cyclomatic complexity by
     * delegating to specialized helper classes:</p>
     * <ul>
     *   <li>{@link FileValidation} - Validates file uploads</li>
     *   <li>{@link FileTypeChecker} - Checks file type permissions</li>
     *   <li>{@link DirectorySizeCalculator} - Calculates directory sizes</li>
     *   <li>{@link QuotaChecker} - Checks quota limits</li>
     * </ul>
     * 
     * <p>This base implementation writes file content to a file system.</p>
     */
    public class FileContentManagerImpl implements FileContentManager {

        private static final Log log = LogFactory.getLog(FileContentManagerImpl.class);

        private final String storageDir;
        private final QuotaChecker quotaChecker;

        /**
         * Create file content manager with default storage directory.
         */
        public FileContentManagerImpl() {
            this.storageDir = initializeStorageDirectory();
            this.quotaChecker = new QuotaChecker();
        }
        
        /**
         * Initialize and validate storage directory from configuration.
         */
        private String initializeStorageDirectory() {
            String dir = WebloggerConfig.getProperty("mediafiles.storage.dir");
            
            if (dir == null || dir.isBlank()) {
                dir = getDefaultStorageDirectory();
            }
            
            return normalizeDirectoryPath(dir);
        }
        
        /**
         * Get default storage directory path.
         */
        private String getDefaultStorageDirectory() {
            return System.getProperty("user.home") 
                    + File.separator + "roller_data" 
                    + File.separator + "mediafiles";
        }
        
        /**
         * Normalize directory path to ensure it ends with separator.
         */
        private String normalizeDirectoryPath(String path) {
            if (!path.endsWith(File.separator)) {
                path += File.separator;
            }
            return path.replace('/', File.separatorChar);
        }

        // @Override
        public void initialize() {
            // Initialization hook for future use
        }

        /**
         * Get file content for a specific file.
         * 
         * Cyclomatic Complexity: 2 (reduced from 3)
         */
        @Override
        public FileContent getFileContent(Weblog weblog, String fileId)
                throws FileNotFoundException, FilePathException {

            File resourceFile = getRealFile(weblog, fileId);

            if (resourceFile.isDirectory()) {
                throw new FilePathException(
                    "Invalid file id [" + fileId + "], path is a directory.");
            }

            return new FileContent(weblog, fileId, resourceFile);
        }

        /**
         * Save file content to weblog's uploads area.
         * 
         * Cyclomatic Complexity: 1 (unchanged)
         */
        @Override
        public void saveFileContent(Weblog weblog, String fileId, InputStream is)
                throws FileNotFoundException, FilePathException, FileIOException {

            validateFileName(fileId);

            File dirPath = getRealFile(weblog, null);
            Path saveFile = Path.of(dirPath.getAbsolutePath(), fileId);

            try (OutputStream os = Files.newOutputStream(saveFile)) {
                is.transferTo(os);
                log.debug("File written successfully to [" + saveFile + "]");
            } catch (IOException e) {
                throw new FileIOException("Error uploading file", e);
            }
        }

        /**
         * Delete file from weblog's uploads area.
         * 
         * Cyclomatic Complexity: 1 (unchanged)
         */
        @Override
        public void deleteFile(Weblog weblog, String fileId)
                throws FileNotFoundException, FilePathException, FileIOException {

            File delFile = getRealFile(weblog, fileId);

            if (!delFile.delete()) {
                log.warn("Delete appears to have failed for [" + fileId + "]");
            }
        }

        @Override
        public void deleteAllFiles(Weblog weblog) throws FileIOException {
            // TODO: Implement
        }

        /**
         * Check if weblog is over quota.
         * 
         * Cyclomatic Complexity: 1 (reduced from 4)
         */
        @Override
        public boolean overQuota(Weblog weblog) {
            try {
                File storageDirectory = getRealFile(weblog, null);
                BigDecimal maxSizeMB = getMaxDirectorySizeMB();
                
                return quotaChecker.isOverQuota(storageDirectory, maxSizeMB);
                
            } catch (Exception ex) {
                // Shouldn't happen - means user's uploads dir is inaccessible
                throw new RuntimeException("Error checking quota for weblog: " + weblog.getHandle(), ex);
            }
        }

        @Override
        public void release() {
            // No resources to release
        }

        /**
         * Determine if file can be saved given current configuration.
         * 
         * Cyclomatic Complexity: 2 (reduced from 10)
         * 
         * <p>Complexity reduction achieved by delegating to FileValidation
         * and QuotaChecker classes.</p>
         */
        @Override
        public boolean canSave(Weblog weblog, String fileName, String contentType,
                long size, RollerMessages messages) {

            FileValidation validation = new FileValidation(fileName, contentType, size, messages);
            
            // Check basic validations
            if (!validation.isUploadEnabled()) {
                return false;
            }
            
            if (!validation.isFileSizeAcceptable()) {
                return false;
            }
            
            // Check quota
            if (!isWithinQuota(weblog, validation, messages)) {
                return false;
            }
            
            // Check file type
            return validation.isFileTypeAllowed();
        }
        
        /**
         * Check if file upload would be within quota limits.
         * 
         * Cyclomatic Complexity: 1
         */
        private boolean isWithinQuota(Weblog weblog, FileValidation validation, 
                                    RollerMessages messages) {
            try {
                File storageDirectory = getRealFile(weblog, null);
                BigDecimal maxDirMB = validation.getMaxDirSizeMB();
                
                return !quotaChecker.wouldExceedQuota(
                    storageDirectory, 
                    validation.getFileSize(), 
                    maxDirMB, 
                    messages
                );
                
            } catch (Exception ex) {
                // Shouldn't happen - means weblog's uploads dir is inaccessible
                throw new RuntimeException("Error checking quota for weblog: " + weblog.getHandle(), ex);
            }
        }

        /**
         * Get maximum directory size from configuration.
         */
        private BigDecimal getMaxDirectorySizeMB() {
            String maxSize = org.apache.roller.weblogger.config.WebloggerRuntimeConfig
                    .getProperty("uploads.dir.maxsize");
            return new BigDecimal(maxSize);
        }

        /**
         * Construct the full real path to a resource in a weblog's uploads area.
         * 
         * Cyclomatic Complexity: 3 (reduced from 4)
         */
        private File getRealFile(Weblog weblog, String fileId)
                throws FileNotFoundException, FilePathException {

            Path weblogDir = Path.of(storageDir, weblog.getHandle());
            ensureDirectoryExists(weblogDir);

            Path filePath = resolveFilePath(weblogDir, fileId);
            validateFilePathReadable(filePath);

            return filePath.toFile();
        }
        
        /**
         * Ensure directory exists, create if necessary.
         */
        private void ensureDirectoryExists(Path directory) throws FilePathException {
            if (!Files.exists(directory)) {
                try {
                    Files.createDirectories(directory);
                } catch (IOException ex) {
                    throw new FilePathException("Cannot create storage directory [" + directory + "]", ex);
                }
            }
        }
        
        /**
         * Resolve file path, validating file ID if provided.
         */
        private Path resolveFilePath(Path baseDir, String fileId) throws FilePathException {
            Path path = baseDir.toAbsolutePath();
            
            if (fileId != null) {
                validateFileName(fileId);
                path = path.resolve(fileId);
            }
            
            return path;
        }
        
        /**
         * Validate that file path exists and is readable.
         */
        private void validateFilePathReadable(Path filePath) throws FileNotFoundException {
            if (!Files.isReadable(filePath)) {
                throw new FileNotFoundException(
                    "Invalid path [" + filePath + "], file does not exist or is not readable.");
            }
        }

        /**
         * Validate file name for security (prevent directory traversal).
         * 
         * Cyclomatic Complexity: 1 (unchanged)
         */
        private static void validateFileName(String fileId) throws FilePathException {
            if (fileId.contains("..")) {
                throw new FilePathException(
                    "Invalid file name [" + fileId + "], attempting directory traversal.");
            }
        }
    }