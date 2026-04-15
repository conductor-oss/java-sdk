/*
 * Copyright 2026 Conductor Authors.
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
package org.conductoross.conductor.client.model.file;

/** Server-authoritative upload lifecycle state. Persisted on the server and mirrored in DTOs. */
public enum FileUploadStatus {
    /** Reserved for future use (e.g. pre-initialized records); not entered by the current flow. */
    PENDING,
    /** Record created; byte transfer not yet confirmed by {@code POST /upload-complete}. */
    UPLOADING,
    /** Terminal success state — content verified present on the storage backend. */
    UPLOADED,
    /**
     * Terminal failure — set by the background audit when an {@link #UPLOADING} record remains
     * stale past the configured threshold.
     */
    FAILED
}
