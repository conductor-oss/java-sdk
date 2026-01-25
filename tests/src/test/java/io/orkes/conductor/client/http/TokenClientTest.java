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
package io.orkes.conductor.client.http;

import java.io.IOException;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.orkes.conductor.client.model.GenerateTokenRequest;
import io.orkes.conductor.client.model.TokenResponse;
import io.orkes.conductor.client.util.ClientTestUtil;

public class TokenClientTest {

    public static OrkesTokenClient tokenClient;

    @BeforeAll
    public static void setup() throws IOException {
        tokenClient = ClientTestUtil.getOrkesClients().getTokenClient();
    }

    @Test
    public void testGenerateToken() {
        String conductorAuthKey = System.getenv("CONDUCTOR_AUTH_KEY");
        String conductorAuthSecret = System.getenv("CONDUCTOR_AUTH_SECRET");
        var req = new GenerateTokenRequest(
                conductorAuthKey,
                conductorAuthSecret
        );
        TokenResponse tokenResponse = tokenClient.generateToken(req);

        assert tokenResponse != null;
        assert tokenResponse.getToken() != null;

        var userInfo = tokenClient.getUserInfo();

        assert userInfo != null;
        assert userInfo.getId() != null;
        assert userInfo.getName() != null;
    }
}
