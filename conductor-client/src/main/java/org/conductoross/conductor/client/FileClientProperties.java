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

import java.nio.file.Path;

/**
 * Configuration for {@link FileClient}. Populated from {@code conductor.file-client.*}
 * properties when using Spring auto-configuration.
 */
public class FileClientProperties {

    /** Upload and download retry count on transient failures. Default: 3. */
    private int retryCount = 3;

    /**
     * Local directory under which downloaded content is cached. Default:
     * {@code ${java.io.tmpdir}/conductor/files-cache}. Files are never deleted — by design
     * the SDK does not clean up cached content.
     */
    private String localCacheDirectory =
            Path.of(getTempDirectory(), "conductor", "files-cache").toString();

    /** Multipart upload part size in bytes. Default: 10 MiB (S3 minimum). */
    private long multipartPartSize = 10L * 1024 * 1024;

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public String getLocalCacheDirectory() {
        return localCacheDirectory;
    }

    public void setLocalCacheDirectory(String localCacheDirectory) {
        this.localCacheDirectory = localCacheDirectory;
    }

    public long getMultipartPartSize() {
        return multipartPartSize;
    }

    public void setMultipartPartSize(long multipartPartSize) {
        this.multipartPartSize = multipartPartSize;
    }

    private static String getTempDirectory() {
        return System.getProperty("java.io.tmpdir");
    }
}
