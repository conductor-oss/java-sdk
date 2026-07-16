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
package org.conductoross.conductor.ai.model;

/**
 * A credential that should be written to a file in the subprocess HOME directory.
 *
 * <pre>{@code
 * CredentialFile kubeconfig = new CredentialFile("KUBECONFIG", ".kube/config");
 * }</pre>
 */
public final class CredentialFile {

    private final String envVar;
    private final String relativePath;
    private final String content;

    public CredentialFile(String envVar, String relativePath) {
        this(envVar, relativePath, null);
    }

    public CredentialFile(String envVar, String relativePath, String content) {
        this.envVar = envVar;
        this.relativePath = relativePath;
        this.content = content;
    }

    /** Environment variable name that will point to the resolved file path (e.g. {@code "KUBECONFIG"}). */
    public String getEnvVar() {
        return envVar;
    }

    /** Path relative to the subprocess temp HOME directory (e.g. {@code ".kube/config"}). */
    public String getRelativePath() {
        return relativePath;
    }

    /** File content (set by fetcher after resolving). {@code null} means not yet resolved. */
    public String getContent() {
        return content;
    }

    public CredentialFile withContent(String content) {
        return new CredentialFile(envVar, relativePath, content);
    }

    @Override
    public String toString() {
        return "CredentialFile{envVar=" + envVar + ", relativePath=" + relativePath + "}";
    }
}
