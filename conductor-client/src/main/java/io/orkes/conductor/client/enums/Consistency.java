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
package io.orkes.conductor.client.enums;

/**
 * @deprecated Use {@link org.conductoross.conductor.common.model.Consistency}
 *             instead.
 *             This class is kept for backwards compatibility.
 */
@Deprecated
public enum Consistency {
    SYNCHRONOUS,
    DURABLE, // Default
    REGION_DURABLE;

    /**
     * Convert to the new Consistency type in conductor-client
     */
    public org.conductoross.conductor.common.model.Consistency toClientConsistency() {
        return org.conductoross.conductor.common.model.Consistency.valueOf(this.name());
    }

    /**
     * Convert from the new Consistency type in conductor-client
     */
    public static Consistency fromClientConsistency(org.conductoross.conductor.common.model.Consistency consistency) {
        return Consistency.valueOf(consistency.name());
    }
}
