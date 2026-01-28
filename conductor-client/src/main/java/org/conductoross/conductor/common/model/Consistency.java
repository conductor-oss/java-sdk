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
package org.conductoross.conductor.common.model;

public enum Consistency {
    // Request is kept in memory until the evaluation completes and flushed to the persistence afterwards
    // If the node dies before the writes are flushed, the workflow request is gone
    SYNCHRONOUS,

    // Default
    // Request is stored in a persistence store before the evaluation - Guarantees the execution
    // Implements durable workflows -- Suitable for most use cases
    DURABLE,

    // In the multi-region setup, guarantees that the start request is replicated across the region
    // Safest
    // Slowest
    REGION_DURABLE
}
