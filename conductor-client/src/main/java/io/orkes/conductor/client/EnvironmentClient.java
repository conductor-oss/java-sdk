/*
 * Copyright 2025 Conductor Authors.
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

import io.orkes.conductor.client.model.Tag;
import io.orkes.conductor.client.model.environment.EnvironmentVariable;

public interface EnvironmentClient {

    /**
     * List all environment variables (each item includes name, value and tags if present).
     * @return list of environment variables
     */
    java.util.List<EnvironmentVariable> getAllEnvironmentVariables();

    /**
     * Get value of an environment variable.
     * @param key variable name (spec path placeholder {key})
     * @return plain text value (may be empty string if stored as such)
     */
    String getEnvironmentVariable(String key);

    /**
     * Create or update an environment variable's value.
     * @param key variable name (spec path placeholder {key})
     * @param value plain text value
     */
    void createOrUpdateEnvironmentVariable(String key, String value);

    /**
     * Delete an environment variable.
     * @param key variable name (spec path placeholder {key})
     * @return the previous value (as returned by server) or null if none
     */
    String deleteEnvironmentVariable(String key);

    /**
     * Get tags for the environment variable.
     * @param name variable name (spec path placeholder {name})
     * @return list of tags (never null)
     */
    List<Tag> getEnvironmentVariableTags(String name);

    /**
     * Put (add or replace) tags for the environment variable.
     * @param name variable name (spec path placeholder {name})
     * @param tags list of tags
     */
    void setEnvironmentVariableTags(String name, List<Tag> tags);

    /**
     * Delete given tags for the environment variable.
     * @param name variable name (spec path placeholder {name})
     * @param tags list of tags to delete
     */
    void deleteEnvironmentVariableTags(String name, List<Tag> tags);
}