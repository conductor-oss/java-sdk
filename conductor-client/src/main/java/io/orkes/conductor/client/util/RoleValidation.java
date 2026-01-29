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
package io.orkes.conductor.client.util;

import java.util.Map;
import java.util.Set;

public final class RoleValidation {

    private RoleValidation() {}

    private static final Set<String> APP_ASSIGNABLE_ROLES = Set.of("APPLICATION_CREATOR", "WORKER", "METADATA_API");

    private static final Set<String> GROUP_DEFAULT_TARGET_TYPES = Set.of("WORKFLOW_DEF", "TASK_DEF", "WORKFLOW_SCHEDULE");

    public static boolean isAssignableToApplication(String role) {
        if (role == null) return false;
        return APP_ASSIGNABLE_ROLES.contains(role.toUpperCase());
    }

    public static void validateGroupDefaultAccessKeys(Map<String, ?> defaultAccess) {
        if (defaultAccess == null || defaultAccess.isEmpty()) return;
        for (String key : defaultAccess.keySet()) {
            if (!GROUP_DEFAULT_TARGET_TYPES.contains(key)) {
                throw new IllegalArgumentException(
                    "Unsupported target type " + key + ", supported: WORKFLOW_DEF, TASK_DEF, WORKFLOW_SCHEDULE");
            }
        }
    }
}
