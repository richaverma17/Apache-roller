package org.apache.roller.weblogger.business;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.FileContent;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.util.RollerMessages;

public class FileContentManagerImpl implements FileContentManager {

    private static final Log log =
            LogFactory.getLog(FileContentManagerImpl.class);

    private String storageDir;

    public FileContentManagerImpl() {

        String inStorageDir =
                WebloggerConfig.getProperty("mediafiles.storage.dir");

        if (inStorageDir == null || inStorageDir.isBlank()) {
            inStorageDir = System.getProperty("user.home")
                    + File.separator + "roller_data"
                    + File.separator + "mediafiles";
        }

        if (!inStorageDir.endsWith(File.separator)) {
            inStorageDir += File.separator;
        }

        this.storageDir = inStorageDir.replace('/', File.separatorChar);
    }

    // @Override
    public void initialize() {
        // no-op
    }
    

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

    @Override
    public void saveFileContent(Weblog weblog, String fileId, InputStream is)
            throws FileNotFoundException, FilePathException, FileIOException {

        checkFileName(fileId);

        File dirPath = getRealFile(weblog, null);
        Path saveFile = Path.of(dirPath.getAbsolutePath(), fileId);

        try (OutputStream os = Files.newOutputStream(saveFile)) {
            is.transferTo(os);
            log.debug("File written to [" + saveFile + "]");
        } catch (IOException e) {
            throw new FileIOException("ERROR uploading file", e);
        }
    }

    @Override
    public void deleteFile(Weblog weblog, String fileId)
            throws FileNotFoundException, FilePathException, FileIOException {

        File delFile = getRealFile(weblog, fileId);

        if (!delFile.delete()) {
            log.warn("Delete failed for [" + fileId + "]");
        }
    }

    @Override
    public void deleteAllFiles(Weblog weblog) throws FileIOException {
        // not implemented
    }

    @Override
    public boolean overQuota(Weblog weblog) {

        try {
            BigDecimal maxDirMB = new BigDecimal(
                    WebloggerRuntimeConfig.getProperty("uploads.dir.maxsize"));

            long maxBytes = (long) (RollerConstants.ONE_MB_IN_BYTES
                    * maxDirMB.doubleValue());

            File dir = getRealFile(weblog, null);
            long currentSize = getDirSize(dir, true);

            return currentSize > maxBytes;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void release() {
        // no-op
    }

    // --------------------------------------------------------------------
    //  Reduced Cyclomatic Complexity: canSave()
    // --------------------------------------------------------------------

    @Override
    public boolean canSave(Weblog weblog, String fileName,
            String contentType, long size, RollerMessages messages) {

        if (!isUploadEnabled(messages)) return false;
        if (!isFileSizeAllowed(fileName, size, messages)) return false;
        if (!isWithinQuota(weblog, size, messages)) return false;
        if (!isFileTypeAllowed(fileName, contentType, messages)) return false;

        return true;
    }

    private boolean isUploadEnabled(RollerMessages messages) {
        if (!WebloggerRuntimeConfig.getBooleanProperty("uploads.enabled")) {
            messages.addError("error.upload.disabled");
            return false;
        }
        return true;
    }

    private boolean isFileSizeAllowed(String fileName, long size,
            RollerMessages messages) {

        BigDecimal maxFileMB = new BigDecimal(
                WebloggerRuntimeConfig.getProperty("uploads.file.maxsize"));

        long maxBytes = (long) (RollerConstants.ONE_MB_IN_BYTES
                * maxFileMB.doubleValue());

        if (size > maxBytes) {
            messages.addError("error.upload.filemax",
                    new String[]{fileName, maxFileMB.toString()});
            return false;
        }
        return true;
    }

    private boolean isWithinQuota(Weblog weblog, long size,
            RollerMessages messages) {

        try {
            BigDecimal maxDirMB = new BigDecimal(
                    WebloggerRuntimeConfig.getProperty("uploads.dir.maxsize"));

            long maxBytes = (long) (RollerConstants.ONE_MB_IN_BYTES
                    * maxDirMB.doubleValue());

            File dir = getRealFile(weblog, null);
            long currentSize = getDirSize(dir, true);

            if (currentSize + size > maxBytes) {
                messages.addError("error.upload.dirmax",
                        maxDirMB.toString());
                return false;
            }
            return true;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isFileTypeAllowed(String fileName,
            String contentType, RollerMessages messages) {

        String allows = WebloggerRuntimeConfig
                .getProperty("uploads.types.allowed");
        String forbids = WebloggerRuntimeConfig
                .getProperty("uploads.types.forbid");

        String[] allowFiles = StringUtils.split(
                StringUtils.deleteWhitespace(allows), ",");
        String[] forbidFiles = StringUtils.split(
                StringUtils.deleteWhitespace(forbids), ",");

        if (!checkFileType(allowFiles, forbidFiles,
                fileName, contentType)) {

            messages.addError("error.upload.forbiddenFile",
                    new String[]{fileName, contentType});
            return false;
        }
        return true;
    }

    // --------------------------------------------------------------------
    //  Reduced Cyclomatic Complexity: checkFileType()
    // --------------------------------------------------------------------

    private boolean checkFileType(String[] allowFiles,
            String[] forbidFiles,
            String fileName, String contentType) {

        if (!isValidContentType(contentType)) {
            return false;
        }

        boolean allowed =
                isAllowedByRules(allowFiles, fileName, contentType);
        boolean forbidden =
                isForbiddenByRules(forbidFiles, fileName, contentType);

        return allowed && !forbidden;
    }

    private boolean isValidContentType(String contentType) {
        return contentType != null && contentType.contains("/");
    }

    private boolean isAllowedByRules(String[] rules,
            String fileName, String contentType) {

        if (rules == null || rules.length == 0) {
            return true;
        }
        return matchesAnyRule(rules, fileName, contentType);
    }

    private boolean isForbiddenByRules(String[] rules,
            String fileName, String contentType) {

        if (rules == null || rules.length == 0) {
            return false;
        }
        return matchesAnyRule(rules, fileName, contentType);
    }

    private boolean matchesAnyRule(String[] rules,
            String fileName, String contentType) {

        for (String rule : rules) {
            if (isExtensionRule(rule)
                    && fileName.toLowerCase()
                            .endsWith(rule.toLowerCase())) {
                return true;
            }
            if (isContentTypeRule(rule)
                    && matchContentType(rule, contentType)) {
                return true;
            }
        }
        return false;
    }

    private boolean isExtensionRule(String rule) {
        return !rule.contains("/");
    }

    private boolean isContentTypeRule(String rule) {
        return rule.contains("/");
    }

    private boolean matchContentType(String rule, String contentType) {

        if (rule.equals("*/*") || rule.equals(contentType)) {
            return true;
        }

        String[] ruleParts = rule.split("/");
        String[] typeParts = contentType.split("/");

        return ruleParts[0].equals(typeParts[0])
                && "*".equals(ruleParts[1]);
    }

    // --------------------------------------------------------------------
    //  Utilities
    // --------------------------------------------------------------------

    private long getDirSize(File dir, boolean recurse) {

        if (!dir.exists() || !dir.isDirectory() || !dir.canRead()) {
            return 0;
        }

        long size = 0;
        File[] files = dir.listFiles();
        if (files == null) return 0;

        for (File file : files) {
            size += (file.isDirectory() && recurse)
                    ? getDirSize(file, true)
                    : file.length();
        }
        return size;
    }

    private File getRealFile(Weblog weblog, String fileId)
            throws FileNotFoundException, FilePathException {

        Path weblogDir = Path.of(storageDir, weblog.getHandle());

        try {
            Files.createDirectories(weblogDir);
        } catch (IOException e) {
            throw new FilePathException(
                    "Can't create storage dir [" + weblogDir + "]", e);
        }

        Path filePath = weblogDir.toAbsolutePath();
        if (fileId != null) {
            checkFileName(fileId);
            filePath = filePath.resolve(fileId);
        }

        if (!Files.isReadable(filePath)) {
            throw new FileNotFoundException(
                    "Invalid path [" + filePath + "]");
        }

        return filePath.toFile();
    }

    private static void checkFileName(String fileId)
            throws FilePathException {

        if (fileId.contains("..")) {
            throw new FilePathException(
                    "Invalid file name [" + fileId + "]");
        }
    }
}
