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

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;

import okhttp3.OkHttpClient;

final class AzureFileTransferAdapter implements FileTransferAdapter {

    private static final Map<String, String> BLOCK_BLOB_HEADER =
            Map.of("x-ms-blob-type", "BlockBlob");

    private final SignedUrlHttpTransfer http;

    AzureFileTransferAdapter(OkHttpClient client) {
        this.http = new SignedUrlHttpTransfer(client);
    }

    @Override
    public String storageType() { return "AZURE_BLOB"; }

    @Override
    public void upload(String signedUrl, Path source) throws IOException {
        http.upload(signedUrl, source, BLOCK_BLOB_HEADER);
    }

    @Override
    public void download(String signedUrl, Path destination) throws IOException {
        http.download(signedUrl, destination);
    }

    @Override
    public boolean supportsMultipart() { return true; }

    @Override
    public String uploadPart(
            String signedUrl, Path source, long offset, long length, int partNumber)
            throws IOException {
        String blockId = Base64.getEncoder().encodeToString(
                String.format(Locale.ROOT, "block-%08d", partNumber)
                        .getBytes(StandardCharsets.UTF_8));
        String blockUrl = http.urlWithQuery(
                signedUrl, Map.of("comp", "block", "blockid", blockId)).toString();
        http.uploadRange(blockUrl, source, offset, length, Map.of());
        return blockId;
    }
}
