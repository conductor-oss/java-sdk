package io.orkes.conductor.client.http;

import io.orkes.conductor.client.model.GenerateTokenRequest;
import io.orkes.conductor.client.model.TokenResponse;
import io.orkes.conductor.client.util.ClientTestUtil;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

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
