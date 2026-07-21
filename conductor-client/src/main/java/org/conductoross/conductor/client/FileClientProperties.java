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
package org.conductoross.conductor.client;

/**
 * Configuration for {@link FileClient}. Populated from {@code conductor.file-client.*}
 * properties when using Spring auto-configuration.
 */
public class FileClientProperties {

    /** Number of retries after the initial signed transfer attempt. Default: 3. */
    private int retryCount = 3;

    /** Multipart upload part size in bytes. Default: 10 MiB. */
    private long multipartPartSize = 10L * 1024 * 1024;

    /** Files larger than this value use multipart when the provider supports it. Default: 100 MiB. */
    private long multipartThreshold = 100L * 1024 * 1024;

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public long getMultipartPartSize() {
        return multipartPartSize;
    }

    public void setMultipartPartSize(long multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }

    public long getMultipartThreshold() {
        return multipartThreshold;
    }

    public void setMultipartThreshold(long multipartThreshold) {
        this.multipartThreshold = multipartThreshold;
    }
}
