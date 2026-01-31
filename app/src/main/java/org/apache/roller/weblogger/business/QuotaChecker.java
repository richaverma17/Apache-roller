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

import org.apache.roller.weblogger.util.RollerMessages;

import java.io.File;
import java.math.BigDecimal;

/**
 * Checks if file uploads would exceed quota limits.
 * 
 * <p>Reduces cyclomatic complexity by extracting quota checking
 * logic into a focused class.</p>
 */
class QuotaChecker {
    
    private final DirectorySizeCalculator sizeCalculator;
    
    /**
     * Construct a quota checker.
     */
    QuotaChecker() {
        this.sizeCalculator = new DirectorySizeCalculator();
    }
    
    /**
     * Check if directory is over quota.
     * 
     * @param directory the directory to check
     * @param maxSizeMB maximum size in megabytes
     * @return true if over quota, false otherwise
     */
    boolean isOverQuota(File directory, BigDecimal maxSizeMB) {
        long maxBytes = convertMBToBytes(maxSizeMB);
        long currentSize = sizeCalculator.calculateSize(directory, true);
        return currentSize > maxBytes;
    }
    
    /**
     * Check if adding a file would exceed quota.
     * 
     * @param directory the directory to check
     * @param fileSize size of file to add
     * @param maxSizeMB maximum directory size in MB
     * @param messages messages to add errors to
     * @return true if within quota, false if would exceed
     */
    boolean wouldExceedQuota(File directory, long fileSize, 
                            BigDecimal maxSizeMB, RollerMessages messages) {
        long maxBytes = convertMBToBytes(maxSizeMB);
        long currentSize = sizeCalculator.calculateSize(directory, true);
        
        if (currentSize + fileSize > maxBytes) {
            messages.addError("error.upload.dirmax", maxSizeMB.toString());
            return true;
        }
        return false;
    }
    
    /**
     * Convert megabytes to bytes.
     */
    private long convertMBToBytes(BigDecimal mb) {
        return (long) (org.apache.roller.util.RollerConstants.ONE_MB_IN_BYTES * mb.doubleValue());
    }
}
