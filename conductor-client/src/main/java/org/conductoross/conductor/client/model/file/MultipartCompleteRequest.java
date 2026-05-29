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

import java.util.List;

import org.conductoross.conductor.sdk.file.FileStorageBackend;

/**
 * Payload for {@code POST /api/files/{fileId}/multipart/{uploadId}/complete}. {@code partETags}
 * is the ordered list of ETags (or backend-equivalent identifiers) returned by each
 * {@link FileStorageBackend#uploadPart uploadPart} call.
 */
public class MultipartCompleteRequest {

    private List<String> partETags;

    public List<String> getPartETags() { return partETags; }
    public void setPartETags(List<String> partETags) { this.partETags = partETags; }
}
