/*
 * Copyright 2022 Conductor Authors.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package io.orkes.conductor.client;

import java.util.List;
import java.util.Map;

import io.orkes.conductor.client.model.AccessKeyResponse;
import io.orkes.conductor.client.model.AuthorizationRequest;
import io.orkes.conductor.client.model.ConductorApplication;
import io.orkes.conductor.client.model.ConductorUser;
import io.orkes.conductor.client.model.CreateAccessKeyResponse;
import io.orkes.conductor.client.model.CreateOrUpdateApplicationRequest;
import io.orkes.conductor.client.model.GrantedAccessResponse;
import io.orkes.conductor.client.model.Group;
import io.orkes.conductor.client.model.Subject;
import io.orkes.conductor.client.model.TagObject;
import io.orkes.conductor.client.model.UpsertGroupRequest;
import io.orkes.conductor.client.model.UpsertUserRequest;

/**
 * Client for managing authorization, users, groups, and applications in
 * Conductor.
 * <p>
 * This client provides comprehensive access control management including:
 * <ul>
 * <li><b>Permissions</b> - Grant and revoke access to workflows, tasks, and
 * other resources</li>
 * <li><b>Users</b> - Manage user accounts and their permissions</li>
 * <li><b>Groups</b> - Organize users into groups for easier permission
 * management</li>
 * <li><b>Applications</b> - Manage application identities and their API access
 * keys</li>
 * </ul>
 * <p>
 * <b>Common Permission Targets:</b>
 * WORKFLOW_DEF, TASK_DEF, WORKFLOW, TASK_ID, APPLICATION, USER, SECRET_NAME,
 * TAG, DOMAIN,
 * INTEGRATION_PROVIDER, INTEGRATION, PROMPT, USER_FORM_TEMPLATE, SCHEMA,
 * WEBHOOK, etc.
 */
public interface AuthorizationClient {

    // ==================== Permission Management ====================

    /**
     * Gets all permissions granted on a specific resource.
     * <p>
     * Returns a map of permission types to the subjects (users, groups, or
     * applications)
     * that have been granted those permissions.
     *
     * @param type the resource type (e.g., "WORKFLOW_DEF", "TASK_DEF",
     *             "SECRET_NAME")
     * @param id   the resource identifier (e.g., workflow name, task name, secret
     *             key)
     * @return map of permission type to list of subjects with that permission
     */
    Map<String, List<Subject>> getPermissions(String type, String id);

    /**
     * Grants permissions to a user, group, or application over a target resource.
     * <p>
     * Use this to give READ, EXECUTE, UPDATE, or DELETE permissions on workflows,
     * tasks,
     * secrets, and other resources. Permissions are additive - granting a
     * permission
     * does not remove existing permissions.
     *
     * @param authorizationRequest the authorization request containing target
     *                             resource, subject, and permissions to grant
     */
    void grantPermissions(AuthorizationRequest authorizationRequest);

    /**
     * Removes permissions from a user, group, or application over a target
     * resource.
     * <p>
     * This revokes specific permissions without affecting other permissions the
     * subject may have.
     *
     * @param authorizationRequest the authorization request containing target
     *                             resource, subject, and permissions to remove
     */
    void removePermissions(AuthorizationRequest authorizationRequest);

    // ==================== User Management ====================

    /**
     * Retrieves a user by ID.
     *
     * @param id the user ID
     * @return the user details
     */
    ConductorUser getUser(String id);

    /**
     * Lists all users in the system.
     *
     * @param apps if true, includes application users; if false, only returns human
     *             users
     * @return list of users
     */
    List<ConductorUser> listUsers(Boolean apps);

    /**
     * Creates or updates a user.
     * <p>
     * If a user with the specified ID exists, it will be updated with the new
     * details.
     * Otherwise, a new user will be created.
     *
     * @param upsertUserRequest the user details (name, email, roles, groups, etc.)
     * @param id                the user ID
     * @return the created or updated user
     */
    ConductorUser upsertUser(UpsertUserRequest upsertUserRequest, String id);

    /**
     * Deletes a user from the system.
     * <p>
     * This permanently removes the user account and revokes all their permissions.
     *
     * @param id the user ID to delete
     */
    void deleteUser(String id);

    /**
     * Sends an invitation email to a new user.
     * <p>
     * The user will receive an email with instructions to set up their account.
     *
     * @param email the email address to send the invitation to
     */
    void sendInviteEmail(String email);

    /**
     * Gets all permissions that have been granted to a specific user.
     * <p>
     * This includes both direct permissions and permissions inherited from groups
     * the user belongs to.
     *
     * @param userId the user ID
     * @return granted access response containing all permissions
     */
    GrantedAccessResponse getGrantedPermissionsForUser(String userId);

    // ==================== Group Management ====================

    /**
     * Retrieves a group by ID.
     *
     * @param id the group ID
     * @return the group details
     */
    Group getGroup(String id);

    /**
     * Lists all groups in the system.
     *
     * @return list of all groups
     */
    List<Group> listGroups();

    /**
     * Creates or updates a group.
     * <p>
     * If a group with the specified ID exists, it will be updated. Otherwise, a new
     * group is created.
     *
     * @param upsertGroupRequest the group details (name, description, default
     *                           permissions, etc.)
     * @param id                 the group ID
     * @return the created or updated group
     */
    Group upsertGroup(UpsertGroupRequest upsertGroupRequest, String id);

    /**
     * Deletes a group.
     * <p>
     * Users in the group will lose any permissions they inherited from the group,
     * but their direct permissions remain intact.
     *
     * @param id the group ID to delete
     */
    void deleteGroup(String id);

    /**
     * Adds a user to a group.
     * <p>
     * The user will inherit all permissions assigned to the group.
     *
     * @param groupId the group ID
     * @param userId  the user ID to add
     */
    void addUserToGroup(String groupId, String userId);

    /**
     * Removes a user from a group.
     * <p>
     * The user will lose permissions inherited from the group but retain any direct
     * permissions.
     *
     * @param groupId the group ID
     * @param userId  the user ID to remove
     */
    void removeUserFromGroup(String groupId, String userId);

    /**
     * Gets all users that belong to a specific group.
     *
     * @param id the group ID
     * @return list of users in the group
     */
    List<ConductorUser> getUsersInGroup(String id);

    /**
     * Gets all permissions that have been granted to a specific group.
     * <p>
     * All users in this group inherit these permissions.
     *
     * @param groupId the group ID
     * @return granted access response containing all permissions
     */
    GrantedAccessResponse getGrantedPermissionsForGroup(String groupId);

    // ==================== Application Management ====================

    /**
     * Retrieves an application by ID.
     *
     * @param id the application ID
     * @return the application details
     */
    ConductorApplication getApplication(String id);

    /**
     * Retrieves an application by its access key ID.
     * <p>
     * Useful for looking up which application owns a particular access key.
     *
     * @param accessKeyId the access key ID
     * @return the application that owns this access key
     */
    ConductorApplication getApplicationByAccessKeyId(String accessKeyId);

    /**
     * Lists all applications in the system.
     *
     * @return list of all applications
     */
    List<ConductorApplication> listApplications();

    /**
     * Creates a new application.
     * <p>
     * Applications represent programmatic clients that interact with Conductor via
     * API.
     *
     * @param createOrUpdateApplicationRequest the application details (name, owner,
     *                                         etc.)
     * @return the created application
     */
    ConductorApplication createApplication(CreateOrUpdateApplicationRequest createOrUpdateApplicationRequest);

    /**
     * Updates an existing application.
     *
     * @param createOrUpdateApplicationRequest the updated application details
     * @param id                               the application ID
     * @return the updated application
     */
    ConductorApplication updateApplication(CreateOrUpdateApplicationRequest createOrUpdateApplicationRequest,
            String id);

    /**
     * Deletes an application.
     * <p>
     * This will revoke all access keys associated with the application and remove
     * all its permissions.
     *
     * @param id the application ID to delete
     */
    void deleteApplication(String id);

    /**
     * Adds a role to an application user.
     * <p>
     * Roles determine what permissions the application has (e.g., "ADMIN",
     * "WORKER", "METADATA_MANAGER").
     *
     * @param applicationId the application ID
     * @param role          the role name to add
     */
    void addRoleToApplicationUser(String applicationId, String role);

    /**
     * Removes a role from an application user.
     *
     * @param applicationId the application ID
     * @param role          the role name to remove
     */
    void removeRoleFromApplicationUser(String applicationId, String role);

    // ==================== Access Key Management ====================

    /**
     * Creates a new access key for an application.
     * <p>
     * Access keys are used for API authentication. The key ID and secret are
     * returned
     * only once when created - store them securely.
     *
     * @param id the application ID
     * @return the created access key with key ID and secret
     */
    CreateAccessKeyResponse createAccessKey(String id);

    /**
     * Gets all access keys for an application.
     * <p>
     * Note: This returns key IDs and metadata, not the actual secrets.
     *
     * @param id the application ID
     * @return list of access keys (without secrets)
     */
    List<AccessKeyResponse> getAccessKeys(String id);

    /**
     * Toggles the status of an access key (enable/disable).
     * <p>
     * Disabled keys cannot be used for authentication. This is useful for rotating
     * keys
     * or temporarily revoking access without deleting the key.
     *
     * @param applicationId the application ID
     * @param keyId         the access key ID
     * @return the updated access key status
     */
    AccessKeyResponse toggleAccessKeyStatus(String applicationId, String keyId);

    /**
     * Deletes an access key.
     * <p>
     * This permanently revokes the key - it cannot be recovered.
     *
     * @param applicationId the application ID
     * @param keyId         the access key ID to delete
     */
    void deleteAccessKey(String applicationId, String keyId);

    // ==================== Application Tag Management ====================

    /**
     * Sets tags for an application.
     * <p>
     * Tags help organize and categorize applications.
     *
     * @param body          list of tags to set
     * @param applicationId the application ID
     */
    void setApplicationTags(List<TagObject> body, String applicationId);

    /**
     * Gets all tags associated with an application.
     *
     * @param applicationId the application ID
     * @return list of tags
     */
    List<TagObject> getApplicationTags(String applicationId);

    /**
     * Deletes specific tags from an application.
     *
     * @param body          list of tags to delete
     * @param applicationId the application ID
     */
    void deleteApplicationTags(List<TagObject> body, String applicationId);
}
