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
import java.util.Arrays;

/**
 * Calculates directory sizes with optional recursion.
 * 
 * <p>Reduces cyclomatic complexity by using streams and
 * cleaner conditional logic.</p>
 */
class DirectorySizeCalculator {
    
    /**
     * Calculate the size of a directory in bytes.
     * 
     * @param directory the directory to measure
     * @param recursive whether to include subdirectories
     * @return size in bytes, or 0 if directory invalid
     */
    long calculateSize(File directory, boolean recursive) {
        if (!isValidDirectory(directory)) {
            return 0;
        }
        
        File[] files = directory.listFiles();
        if (files == null) {
            return 0;
        }
        
        return Arrays.stream(files)
                .mapToLong(file -> getFileSize(file, recursive))
                .sum();
    }
    
    /**
     * Check if directory is valid and accessible.
     */
    private boolean isValidDirectory(File directory) {
        return directory != null 
                && directory.exists() 
                && directory.isDirectory() 
                && directory.canRead();
    }
    
    /**
     * Get size of a single file or directory.
     */
    private long getFileSize(File file, boolean recursive) {
        if (file.isDirectory()) {
            return recursive ? calculateSize(file, true) : 0;
        }
        return file.length();
    }
}
