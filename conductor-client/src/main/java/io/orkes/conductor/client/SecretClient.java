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
import java.util.Set;

import io.orkes.conductor.client.model.TagObject;

/**
 * Client for managing secrets in Conductor.
 * <p>
 * Secrets provide a secure way to store sensitive information like API keys,
 * passwords,
 * and credentials that can be referenced in workflows without exposing them in
 * workflow definitions.
 * <p>
 * Key features:
 * <ul>
 * <li>Store and retrieve encrypted secrets</li>
 * <li>Check secret existence without retrieving the value</li>
 * <li>List available secrets based on user permissions</li>
 * <li>Tag secrets for organization and discovery</li>
 * <li>Secrets are encrypted at rest and in transit</li>
 * </ul>
 * <p>
 * Secret keys must match the pattern: [a-zA-Z0-9_-]+
 */
public interface SecretClient {

    // Secret CRUD operations

    /**
     * Stores or updates a secret value.
     * <p>
     * The secret value is encrypted before storage. If a secret with the same key
     * already exists,
     * it will be overwritten with the new value.
     * <p>
     * Maximum secret value length: 65,535 characters
     *
     * @param value the secret value to store (will be encrypted)
     * @param key   the secret key (must match pattern: [a-zA-Z0-9_-]+)
     */
    void putSecret(String value, String key);

    /**
     * Retrieves a secret value by key.
     * <p>
     * The secret is decrypted before being returned. The user must have READ access
     * to the secret.
     *
     * @param key the secret key
     * @return the decrypted secret value
     */
    String getSecret(String key);

    /**
     * Deletes a secret by key.
     * <p>
     * This permanently removes the secret from the system. Requires metadata or
     * admin role.
     *
     * @param key the secret key to delete
     */
    void deleteSecret(String key);

    /**
     * Checks if a secret exists without retrieving its value.
     * <p>
     * This is useful for conditional logic in workflows or for verifying secret
     * availability
     * before using it.
     *
     * @param key the secret key to check
     * @return true if the secret exists, false otherwise
     */
    boolean secretExists(String key);

    // List operations

    /**
     * Lists all secret names in the system.
     * <p>
     * This returns all secrets regardless of access permissions. Use this for
     * administrative
     * purposes or when you need to see all secrets in the system.
     *
     * @return set of all secret names
     */
    Set<String> listAllSecretNames();

    /**
     * Lists secret names that the current user can grant access to.
     * <p>
     * This returns only secrets where the user has sufficient permissions to manage
     * access.
     * Useful for building UIs that allow users to share secrets with others.
     *
     * @return list of secret names the user can grant access to
     */
    List<String> listSecretsThatUserCanGrantAccessTo();

    // Tag operations

    /**
     * Adds or updates tags for a secret.
     * <p>
     * Tags help organize and categorize secrets. You can use tags to group secrets
     * by environment, application, team, or any other custom categorization.
     *
     * @param tags list of tags to add or update
     * @param key  the secret key
     */
    void setSecretTags(List<TagObject> tags, String key);

    /**
     * Deletes specific tags from a secret.
     *
     * @param body list of tags to delete
     * @param key  the secret key
     */
    void deleteSecretTags(List<TagObject> body, String key);

    /**
     * Retrieves all tags associated with a secret.
     *
     * @param key the secret key
     * @return list of tags for the secret
     */
    List<TagObject> getSecretTags(String key);
}
