package apicalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Formats the parsed API data into a human-readable answer for the user.
 * Takes userRequest, parsedData, and apiName.
 * Returns a formatted answer string, the data source, and the number of fields used.
 */
public class FormatOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ap_format_output";
    }

    @Override
    public TaskResult execute(Task task) {
        String userRequest = (String) task.getInputData().get("userRequest");
        if (userRequest == null || userRequest.isBlank()) {
            userRequest = "general API request";
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> parsedData = (Map<String, Object>) task.getInputData().get("parsedData");

        String apiName = (String) task.getInputData().get("apiName");
        if (apiName == null || apiName.isBlank()) {
            apiName = "unknown";
        }

        System.out.println("  [ap_format_output] Formatting output for request: " + userRequest);

        int fieldsUsed = 0;
        StringBuilder answer = new StringBuilder();

        if (parsedData != null && !parsedData.isEmpty()) {
            fieldsUsed = parsedData.size();

            String name = String.valueOf(parsedData.getOrDefault("name", "Unknown Repository"));
            String description = String.valueOf(parsedData.getOrDefault("description", "No description available"));
            Object starsObj = parsedData.getOrDefault("stars", 0);
            Object forksObj = parsedData.getOrDefault("forks", 0);
            String language = String.valueOf(parsedData.getOrDefault("language", "Unknown"));
            String license = String.valueOf(parsedData.getOrDefault("license", "Unknown"));

            answer.append("The repository ").append(name).append(" is ").append(description).append(". ");
            answer.append("It is written in ").append(language);
            answer.append(" and has ").append(starsObj).append(" stars");
            answer.append(" and ").append(forksObj).append(" forks. ");
            answer.append("It is licensed under ").append(license).append(".");
        } else {
            answer.append("No data was available to answer the request: ").append(userRequest);
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("answer", answer.toString());
        result.getOutputData().put("dataSource", apiName + "_api");
        result.getOutputData().put("fieldsUsed", fieldsUsed);
        return result;
    }
}
