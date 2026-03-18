package apicalling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Parses and validates the raw API response against the expected schema.
 * Takes rawResponse, statusCode, and expectedSchema.
 * Returns parsedData, fieldsExtracted count, and validationPassed flag.
 */
public class ParseResponseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ap_parse_response";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        Map<String, Object> rawResponse = (Map<String, Object>) task.getInputData().get("rawResponse");

        Object statusCodeObj = task.getInputData().get("statusCode");
        int statusCode = 0;
        if (statusCodeObj instanceof Number) {
            statusCode = ((Number) statusCodeObj).intValue();
        }

        System.out.println("  [ap_parse_response] Parsing response (status code: " + statusCode + ")");

        Map<String, Object> parsedData = new LinkedHashMap<>();
        if (rawResponse != null) {
            if (rawResponse.containsKey("full_name")) {
                parsedData.put("name", rawResponse.get("full_name"));
            }
            if (rawResponse.containsKey("description")) {
                parsedData.put("description", rawResponse.get("description"));
            }
            if (rawResponse.containsKey("stargazers_count")) {
                parsedData.put("stars", rawResponse.get("stargazers_count"));
            }
            if (rawResponse.containsKey("forks_count")) {
                parsedData.put("forks", rawResponse.get("forks_count"));
            }
            if (rawResponse.containsKey("language")) {
                parsedData.put("language", rawResponse.get("language"));
            }
            if (rawResponse.containsKey("license")) {
                Object licenseObj = rawResponse.get("license");
                if (licenseObj instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> licenseMap = (Map<String, Object>) licenseObj;
                    parsedData.put("license", licenseMap.getOrDefault("name", "Unknown"));
                } else {
                    parsedData.put("license", String.valueOf(licenseObj));
                }
            }
            if (rawResponse.containsKey("open_issues_count")) {
                parsedData.put("openIssues", rawResponse.get("open_issues_count"));
            }
            if (rawResponse.containsKey("default_branch")) {
                parsedData.put("defaultBranch", rawResponse.get("default_branch"));
            }
        }

        int fieldsExtracted = parsedData.size();
        boolean validationPassed = statusCode == 200 && fieldsExtracted > 0;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("parsedData", parsedData);
        result.getOutputData().put("fieldsExtracted", fieldsExtracted);
        result.getOutputData().put("validationPassed", validationPassed);
        return result;
    }
}
