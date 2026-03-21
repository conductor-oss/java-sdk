package amazonbedrock.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Calls Amazon Bedrock InvokeModel API.
 *
 * Live Bedrock calls require AWS SDK v2 with SigV4 request signing, which
 * adds significant dependency complexity. This worker always runs in
 * fallback mode with prefix and documents the required env
 * vars for a real integration.
 *
 * To enable live Bedrock calls, add the AWS SDK v2 Bedrock Runtime
 * dependency and implement BedrockRuntimeClient.invokeModel().
 *
 * Required env vars for production:
 *   AWS_ACCESS_KEY_ID     - AWS access key
 *   AWS_SECRET_ACCESS_KEY - AWS secret key
 *   AWS_REGION            - AWS region (e.g. us-east-1)
 *
 * Input: modelId, payload, region
 * Output: responseBody (Claude-on-Bedrock format), metrics
 */
public class BedrockInvokeModelWorker implements Worker {

    private final boolean hasAwsCredentials;

    public BedrockInvokeModelWorker() {
        String accessKey = System.getenv("AWS_ACCESS_KEY_ID");
        String secretKey = System.getenv("AWS_SECRET_ACCESS_KEY");
        this.hasAwsCredentials = accessKey != null && !accessKey.isBlank()
                && secretKey != null && !secretKey.isBlank();

        if (hasAwsCredentials) {
            System.out.println("  [bedrock_invoke_model] AWS credentials detected, but live Bedrock calls "
                    + "require AWS SDK v2 (not included). Running in fallback mode.");
            System.out.println("  [bedrock_invoke_model] To enable live calls, add aws-sdk-java-v2 "
                    + "bedrock-runtime dependency.");
        } else {
            System.out.println("  [bedrock_invoke_model] Fallback mode: set AWS_ACCESS_KEY_ID and "
                    + "AWS_SECRET_ACCESS_KEY for production use (requires AWS SDK v2)");
        }
    }

    @Override
    public String getTaskDefName() {
        return "bedrock_invoke_model";
    }

    @Override
    public TaskResult execute(Task task) {
        String modelId = (String) task.getInputData().get("modelId");

        System.out.println("  [invoke] Calling Bedrock InvokeModel (deterministic.: " + modelId);

        // deterministic Bedrock response in Claude-on-Bedrock format
        Map<String, Object> responseBody = Map.of(
                "id", "msg_bdrk_01ABcDeFgHiJ",
                "type", "message",
                "role", "assistant",
                "content", List.of(
                        Map.of(
                                "type", "text",
                                "text", "Classification: URGENT \u2014 Compliance Risk\n\n"
                                        + "The submitted support ticket indicates a potential data breach "
                                        + "involving PII. Immediate actions required:\n"
                                        + "1. Escalate to Security Operations (SLA: 1 hour)\n"
                                        + "2. Initiate incident response playbook IR-003\n"
                                        + "3. Notify Data Protection Officer within 24 hours per GDPR Article 33\n\n"
                                        + "Confidence: 94%"
                        )
                ),
                "stop_reason", "end_turn",
                "usage", Map.of("input_tokens", 67, "output_tokens", 89)
        );

        Map<String, Object> metrics = Map.of("latencyMs", 1850);

        System.out.println("  [invoke] Response received in " + metrics.get("latencyMs") + "ms");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("responseBody", responseBody);
        result.getOutputData().put("metrics", metrics);
        return result;
    }
}
