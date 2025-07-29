/*
 * Copyright 2022 Conductor Authors.
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
package io.orkes.conductor.client.http;

import com.netflix.conductor.client.http.ConductorClient;
import com.netflix.conductor.client.http.ConductorClientResponse;
import io.orkes.conductor.client.model.ConductorUser;
import io.orkes.conductor.client.model.GenerateTokenRequest;
import io.orkes.conductor.client.model.TokenResponse;

/**
 * Client for token management operations in Orkes Conductor.
 * Provides functionality to manage authentication tokens and user information.
 */
public class OrkesTokenClient {

    private final TokenResource tokenResource;

    public OrkesTokenClient(ConductorClient client) {
        this.tokenResource = new TokenResource(client);
    }

    /**
     * Generate a new authentication token
     *
     * @param request The token generation request containing necessary parameters
     * @return ConductorClientResponse containing the generated token
     */
    public ConductorClientResponse<TokenResponse> generateToken(GenerateTokenRequest request) {
        return tokenResource.generate(request);
    }

    /**
     * Generate a new authentication token with default parameters
     * Convenience method for simple token generation
     *
     * @return ConductorClientResponse containing the generated token
     */
    public ConductorClientResponse<TokenResponse> generateToken() {
        return generateToken(new GenerateTokenRequest());
    }

    /**
     * Get current user information from the token context
     *
     * @return ConductorClientResponse containing user information
     */
    public ConductorClientResponse<ConductorUser> getUserInfo() {
        return tokenResource.getUserInfo();
    }

    /**
     * Convenience method to get user info directly without wrapper
     *
     * @return ConductorUser object or null if failed
     */
    public ConductorUser getCurrentUser() {
        try {
            ConductorClientResponse<ConductorUser> response = getUserInfo();
            return response != null ? response.getData() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if current token/user context is valid
     *
     * @return true if user info can be retrieved successfully, false otherwise
     */
    public boolean isTokenValid() {
        try {
            ConductorClientResponse<ConductorUser> response = getUserInfo();
            return response != null && response.getData() != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate token and extract the token string directly
     * Convenience method for simple use cases
     *
     * @param request The token generation request
     * @return The token string or null if generation failed
     */
    public String generateTokenString(GenerateTokenRequest request) {
        try {
            ConductorClientResponse<TokenResponse> response = generateToken(request);
            return response != null && response.getData() != null ?
                    response.getData().getToken() : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Generate token string with default parameters
     *
     * @return The token string or null if generation failed
     */
    public String generateTokenString() {
        return generateTokenString(new GenerateTokenRequest());
    }
}