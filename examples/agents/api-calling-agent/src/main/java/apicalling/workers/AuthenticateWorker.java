package apicalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;

/**
 * Generates a real JWT (JSON Web Token) using HMAC-SHA256 signing.
 *
 * <p>Creates a proper JWT with three parts: header, payload, and signature. The payload
 * includes standard claims (iss, sub, iat, exp) derived from the input apiName and authType.
 * The token expires after 3600 seconds by default.
 *
 * <p>Uses {@link javax.crypto.Mac} with HmacSHA256 to produce the signature. The signing
 * secret is derived from the apiName for reproducibility in this example context.
 */
public class AuthenticateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ap_authenticate";
    }

    @Override
    public TaskResult execute(Task task) {
        String apiName = (String) task.getInputData().get("apiName");
        if (apiName == null || apiName.isBlank()) {
            apiName = "unknown";
        }

        String authType = (String) task.getInputData().get("authType");
        if (authType == null || authType.isBlank()) {
            authType = "bearer_token";
        }

        System.out.println("  [ap_authenticate] Generating JWT for API: " + apiName + " (auth type: " + authType + ")");

        try {
            long now = Instant.now().getEpochSecond();
            long expiresIn = 3600;
            long exp = now + expiresIn;

            // JWT Header
            String header = "{\"alg\":\"HS256\",\"typ\":\"JWT\"}";

            // JWT Payload
            String payload = "{\"iss\":\"conductor-example\","
                    + "\"sub\":\"" + apiName + "\","
                    + "\"aud\":\"" + authType + "\","
                    + "\"iat\":" + now + ","
                    + "\"exp\":" + exp + "}";

            // Base64url encode header and payload
            String encodedHeader = base64UrlEncode(header.getBytes(StandardCharsets.UTF_8));
            String encodedPayload = base64UrlEncode(payload.getBytes(StandardCharsets.UTF_8));

            String signingInput = encodedHeader + "." + encodedPayload;

            // Sign with HMAC-SHA256
            String secret = "conductor-example-secret-" + apiName;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] signatureBytes = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            String signature = base64UrlEncode(signatureBytes);

            String token = signingInput + "." + signature;

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("token", token);
            result.getOutputData().put("expiresIn", (int) expiresIn);
            result.getOutputData().put("tokenType", "Bearer");
            return result;

        } catch (Exception e) {
            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.getOutputData().put("token", "error-generating-token");
            result.getOutputData().put("expiresIn", 0);
            result.getOutputData().put("tokenType", "Bearer");
            result.getOutputData().put("error", e.getMessage());
            return result;
        }
    }

    /**
     * Base64url encodes the given bytes (no padding), per RFC 7515.
     */
    private static String base64UrlEncode(byte[] data) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(data);
    }
}
