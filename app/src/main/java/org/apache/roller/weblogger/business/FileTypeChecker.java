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

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Checks if file types are allowed based on extension and content-type rules.
 * 
 * <p>Significantly reduces cyclomatic complexity by using streams and
 * functional programming instead of nested loops.</p>
 */
class FileTypeChecker {
    
    private final List<String> allowedRules;
    private final List<String> forbiddenRules;
    
    /**
     * Construct a file type checker with allow and forbid rules.
     */
    FileTypeChecker(String[] allowedRules, String[] forbiddenRules) {
        this.allowedRules = allowedRules != null ? Arrays.asList(allowedRules) : List.of();
        this.forbiddenRules = forbiddenRules != null ? Arrays.asList(forbiddenRules) : List.of();
    }
    
    /**
     * Check if file is allowed based on name and content type.
     * 
     * @param fileName name of the file
     * @param contentType MIME content type
     * @return true if file is allowed, false otherwise
     */
    boolean isAllowed(String fileName, String contentType) {
        // Validate content type format
        if (!isValidContentType(contentType)) {
            return false;
        }
        
        // Check allowed rules
        boolean allowed = isAllowedByRules(fileName, contentType);
        
        // Check forbidden rules (overrides allowed)
        if (allowed && isForbiddenByRules(fileName, contentType)) {
            return false;
        }
        
        return allowed;
    }
    
    /**
     * Check if content type is valid.
     */
    private boolean isValidContentType(String contentType) {
        return contentType != null && contentType.contains("/");
    }
    
    /**
     * Check if file is allowed by allow rules.
     */
    private boolean isAllowedByRules(String fileName, String contentType) {
        // If no allow rules specified, allow all (except those forbidden)
        if (allowedRules.isEmpty()) {
            return true;
        }
        
        // Check if allowed by extension
        if (isAllowedByExtension(fileName)) {
            return true;
        }
        
        // Check if allowed by content type
        return isAllowedByContentType(contentType);
    }
    
    /**
     * Check if file is allowed by file extension rules.
     */
    private boolean isAllowedByExtension(String fileName) {
        return getExtensionRules(allowedRules)
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
    }
    
    /**
     * Check if file is allowed by content type rules.
     */
    private boolean isAllowedByContentType(String contentType) {
        return getContentTypeRules(allowedRules)
                .anyMatch(rule -> matchesContentType(rule, contentType));
    }
    
    /**
     * Check if file is forbidden by forbid rules.
     */
    private boolean isForbiddenByRules(String fileName, String contentType) {
        // Check if forbidden by extension
        if (isForbiddenByExtension(fileName)) {
            return true;
        }
        
        // Check if forbidden by content type
        return isForbiddenByContentType(contentType);
    }
    
    /**
     * Check if file is forbidden by file extension rules.
     */
    private boolean isForbiddenByExtension(String fileName) {
        return getExtensionRules(forbiddenRules)
                .anyMatch(ext -> fileName.toLowerCase().endsWith(ext.toLowerCase()));
    }
    
    /**
     * Check if file is forbidden by content type rules.
     */
    private boolean isForbiddenByContentType(String contentType) {
        return getContentTypeRules(forbiddenRules)
                .anyMatch(rule -> matchesContentType(rule, contentType));
    }
    
    /**
     * Get extension rules (rules without '/').
     */
    private Stream<String> getExtensionRules(List<String> rules) {
        return rules.stream().filter(rule -> !rule.contains("/"));
    }
    
    /**
     * Get content type rules (rules with '/').
     */
    private Stream<String> getContentTypeRules(List<String> rules) {
        return rules.stream().filter(rule -> rule.contains("/"));
    }
    
    /**
     * Check if content type matches a range rule.
     * 
     * <p>Examples:</p>
     * <ul>
     *   <li>Rule "*\/*" matches all content types</li>
     *   <li>Rule "image/*" matches "image/png", "image/jpeg", etc.</li>
     *   <li>Rule "text/xml" matches exactly "text/xml"</li>
     * </ul>
     */
    private boolean matchesContentType(String rangeRule, String contentType) {
        // Match all
        if ("*/*".equals(rangeRule)) {
            return true;
        }
        
        // Exact match
        if (rangeRule.equals(contentType)) {
            return true;
        }
        
        // Wildcard match (e.g., "image/*")
        return matchesWildcard(rangeRule, contentType);
    }
    
    /**
     * Check if content type matches wildcard rule.
     */
    private boolean matchesWildcard(String rangeRule, String contentType) {
        String[] ruleParts = rangeRule.split("/");
        String[] typeParts = contentType.split("/");
        
        if (ruleParts.length != 2 || typeParts.length != 2) {
            return false;
        }
        
        return ruleParts[0].equals(typeParts[0]) && "*".equals(ruleParts[1]);
    }
}
