package cohere.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds a Cohere generate request body from workflow input parameters.
 *
 * Input: productDescription, targetAudience, model, maxTokens, temperature, k, p, numGenerations
 * Output: { requestBody }
 */
public class CohereBuildPromptWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cohere_build_prompt";
    }

    @Override
    public TaskResult execute(Task task) {
        Map<String, Object> input = task.getInputData();

        String productDescription = (String) input.getOrDefault("productDescription", "");
        String targetAudience = (String) input.getOrDefault("targetAudience", "");
        String model = (String) input.getOrDefault("model", "command");
        Number maxTokens = (Number) input.getOrDefault("maxTokens", 300);
        Number temperature = (Number) input.getOrDefault("temperature", 0.9);
        Number k = (Number) input.getOrDefault("k", 0);
        Number p = (Number) input.getOrDefault("p", 0.75);
        Number numGenerations = (Number) input.getOrDefault("numGenerations", 3);

        String prompt = "Write a compelling marketing copy for the following product. "
                + "Target audience: " + targetAudience + ". "
                + "Product: " + productDescription;

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("max_tokens", maxTokens);
        requestBody.put("temperature", temperature);
        requestBody.put("k", k);
        requestBody.put("p", p);
        requestBody.put("num_generations", numGenerations);
        requestBody.put("return_likelihoods", "GENERATION");
        requestBody.put("truncate", "END");

        System.out.println("  [cohere_build_prompt worker] Built request for model: " + model);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestBody", requestBody);
        return result;
    }
}
