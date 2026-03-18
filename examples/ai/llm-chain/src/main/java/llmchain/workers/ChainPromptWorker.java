package llmchain.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Worker 1: Takes customerEmail and productCatalog, builds a structured prompt
 * with few-shot examples and expected JSON format.
 */
public class ChainPromptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "chain_prompt";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerEmail = (String) task.getInputData().get("customerEmail");
        String productCatalog = (String) task.getInputData().get("productCatalog");

        String formattedPrompt = buildPrompt(customerEmail, productCatalog);

        Map<String, Object> expectedFormat = Map.of(
                "intent", "string (inquiry|complaint|return|purchase)",
                "sentiment", "string (positive|neutral|negative)",
                "suggestedProducts", "array of product IDs",
                "draftReply", "string"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("formattedPrompt", formattedPrompt);
        result.getOutputData().put("expectedFormat", expectedFormat);
        result.getOutputData().put("model", "gpt-4");
        return result;
    }

    private String buildPrompt(String customerEmail, String productCatalog) {
        return "You are a customer service AI assistant.\n\n"
                + "Analyze the following customer email and respond with a JSON object.\n\n"
                + "Available products: " + productCatalog + "\n\n"
                + "Few-shot examples:\n"
                + "Example 1 Input: \"I love your product!\"\n"
                + "Example 1 Output: {\"intent\":\"inquiry\",\"sentiment\":\"positive\","
                + "\"suggestedProducts\":[],\"draftReply\":\"Thank you for your kind words!\"}\n\n"
                + "Example 2 Input: \"I want to return my order\"\n"
                + "Example 2 Output: {\"intent\":\"return\",\"sentiment\":\"neutral\","
                + "\"suggestedProducts\":[],\"draftReply\":\"We are sorry to hear that. Let us help you with your return.\"}\n\n"
                + "Expected JSON format:\n"
                + "{\n"
                + "  \"intent\": \"inquiry|complaint|return|purchase\",\n"
                + "  \"sentiment\": \"positive|neutral|negative\",\n"
                + "  \"suggestedProducts\": [\"PROD-ID\"],\n"
                + "  \"draftReply\": \"Your reply here\"\n"
                + "}\n\n"
                + "Customer email:\n" + customerEmail;
    }
}
