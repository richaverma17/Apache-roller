package org.apache.roller.weblogger.business;

import java.math.BigDecimal;

import org.apache.commons.lang3.StringUtils;
import org.apache.roller.util.RollerConstants;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.util.RollerMessages;

/**
 * Validates file uploads based on configured rules.
 *
 * Reduces cyclomatic complexity by extracting validation logic
 * into focused, single-purpose methods.
 */
class FileValidation {

    private final String fileName;
    private final String contentType;
    private final long fileSize;
    private final RollerMessages messages;

    FileValidation(String fileName, String contentType,
                   long fileSize, RollerMessages messages) {
        this.fileName = fileName;
        this.contentType = contentType;
        this.fileSize = fileSize;
        this.messages = messages;
    }

    boolean isUploadEnabled() {
        if (!WebloggerRuntimeConfig.getBooleanProperty("uploads.enabled")) {
            messages.addError("error.upload.disabled");
            return false;
        }
        return true;
    }

    boolean isFileSizeAcceptable() {
        BigDecimal maxFileMB = new BigDecimal(
                WebloggerRuntimeConfig.getProperty("uploads.file.maxsize"));

        long maxBytes = (long) (RollerConstants.ONE_MB_IN_BYTES
                * maxFileMB.doubleValue());

        if (fileSize > maxBytes) {
            messages.addError(
                    "error.upload.filemax",
                    new String[]{fileName, maxFileMB.toString()}
            );
            return false;
        }
        return true;
    }

    boolean isFileTypeAllowed() {

        String[] allowFiles = getAllowedFileTypes();
        String[] forbidFiles = getForbiddenFileTypes();

        FileTypeChecker checker =
                new FileTypeChecker(allowFiles, forbidFiles);

        if (!checker.isAllowed(fileName, contentType)) {
            messages.addError(
                    "error.upload.forbiddenFile",
                    new String[]{fileName, contentType}
            );
            return false;
        }
        return true;
    }

    BigDecimal getMaxDirSizeMB() {
        return new BigDecimal(
                WebloggerRuntimeConfig.getProperty("uploads.dir.maxsize"));
    }

    long getFileSize() {
        return fileSize;
    }

    private String[] getAllowedFileTypes() {
        String allows =
                WebloggerRuntimeConfig.getProperty("uploads.types.allowed");
        return StringUtils.split(
                StringUtils.deleteWhitespace(allows), ",");
    }

    private String[] getForbiddenFileTypes() {
        String forbids =
                WebloggerRuntimeConfig.getProperty("uploads.types.forbid");
        return StringUtils.split(
                StringUtils.deleteWhitespace(forbids), ",");
    }
}
