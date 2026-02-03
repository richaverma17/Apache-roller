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
package org.apache.roller.weblogger.pojos;

import java.security.Permission;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.config.WebloggerConfig;
import org.apache.roller.weblogger.util.Utilities;

/**
 * Represents a permission that applies globally to the entire web application.
 */
public class GlobalPermission extends RollerPermission {

    protected String actions;

    /** Allowed to login and edit profile */
    public static final String LOGIN  = "login";

    /** Allowed to login and do weblogging */
    public static final String WEBLOG = "weblog";

    /** Allowed to login and do everything, including site-wide admin */
    public static final String ADMIN  = "admin";

    /**
     * Create global permission for one specific user initialized with the
     * actions that are implied by the user's roles.
     * @param user User of permission.
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(User user) throws WebloggerException {
        super("GlobalPermission user: " + user.getUserName());
        List<String> userActions = extractActionsFromUserRoles(user);
        setActionsAsList(userActions);
    }

    /**
     * Create global permission with the actions specified by array.
     * @param actions actions to add to permission
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(List<String> actions) throws WebloggerException {
        super("GlobalPermission user: N/A");
        setActionsAsList(actions);
    }

    /**
     * Create global permission for one specific user initialized with the
     * actions specified by array.
     * @param user User of permission.
     * @param actions actions to add to permission
     * @throws org.apache.roller.weblogger.WebloggerException
     */
    public GlobalPermission(User user, List<String> actions) throws WebloggerException {
        super("GlobalPermission user: " + user.getUserName());
        setActionsAsList(actions);
    }

    /**
     * Extracts actions from user's roles by consulting configuration.
     * @param user User whose roles to process
     * @return List of actions implied by user's roles
     * @throws WebloggerException if unable to retrieve roles
     */
    private List<String> extractActionsFromUserRoles(User user) throws WebloggerException {
        List<String> roles = WebloggerFactory.getWeblogger().getUserManager().getRoles(user);
        List<String> actionsList = new ArrayList<>();

        for (String role : roles) {
            addImpliedActionsForRole(role, actionsList);
        }

        return actionsList;
    }

    /**
     * Adds actions implied by a role to the actions list.
     * @param role Role to process
     * @param actionsList List to add actions to
     */
    private void addImpliedActionsForRole(String role, List<String> actionsList) {
        String impliedActions = WebloggerConfig.getProperty("role.action." + role);

        if (impliedActions == null) {
            return;
        }

        List<String> actionsToAdd = Utilities.stringToStringList(impliedActions, ",");
        for (String action : actionsToAdd) {
            if (!actionsList.contains(action)) {
                actionsList.add(action);
            }
        }
    }

    @Override
    public boolean implies(Permission perm) {
        // Early return if no actions defined
        if (getActionsAsList().isEmpty()) {
            return false;
        }

        // Admin has all permissions
        if (hasAction(ADMIN)) {
            return true;
        }

        // Delegate to specific permission type handlers
        if (perm instanceof WeblogPermission) {
            return impliesWeblogPermission();
        }

        if (perm instanceof RollerPermission) {
            return impliesRollerPermission((RollerPermission) perm);
        }

        return false;
    }

    /**
     * Check if this permission implies a WeblogPermission.
     * Admin always implies weblog permissions (already checked in main method).
     * @return true if this permission implies the weblog permission
     */
    private boolean impliesWeblogPermission() {
        // Admin already checked in main implies() method
        return false;
    }

    /**
     * Check if this permission implies a RollerPermission.
     * @param perm The RollerPermission to check
     * @return true if this permission implies the given permission
     */
    private boolean impliesRollerPermission(RollerPermission perm) {
        // Admin already checked in main implies() method

        if (hasAction(WEBLOG)) {
            return !permissionRequiresAdmin(perm);
        }

        if (hasAction(LOGIN)) {
            return permissionRequiresOnlyLogin(perm);
        }

        return true;
    }

    /**
     * Checks if the permission requires admin action.
     * @param perm Permission to check
     * @return true if permission requires admin action
     */
    private boolean permissionRequiresAdmin(RollerPermission perm) {
        return perm.getActionsAsList().contains(ADMIN);
    }

    /**
     * Checks if permission requires only login (no weblog or admin).
     * @param perm Permission to check
     * @return true if permission requires only login action
     */
    private boolean permissionRequiresOnlyLogin(RollerPermission perm) {
        for (String action : perm.getActionsAsList()) {
            if (action.equals(WEBLOG) || action.equals(ADMIN)) {
                return false;
            }
        }
        return true;
    }

    private boolean actionImplies(String action1, String action2) {
        return action1.equals(ADMIN) ||
                (action1.equals(WEBLOG) && action2.equals(LOGIN));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("GlobalPermission: ");
        for (String action : getActionsAsList()) {
            sb.append(" ").append(action).append(" ");
        }
        return sb.toString();
    }

    @Override
    public void setActions(String actions) {
        this.actions = actions;
    }

    @Override
    public String getActions() {
        return actions;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }
        if (!(other instanceof GlobalPermission)) {
            return false;
        }
        GlobalPermission o = (GlobalPermission) other;
        return new EqualsBuilder()
                .append(getActions(), o.getActions())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
                .append(getActions())
                .toHashCode();
    }
}