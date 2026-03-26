package webhookcallback.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Processes the parsed data from the received webhook request. Produces
 * a processing result with record counts and status.
 */
public class ProcessDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wc_process_data";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String requestId = (String) task.getInputData().get("requestId");
        if (requestId == null || requestId.isBlank()) {
            requestId = "unknown";
        }

        Map<String, Object> parsedData = (Map<String, Object>) task.getInputData().get("parsedData");
        if (parsedData == null) {
            parsedData = Map.of();
        }

        System.out.println("  [wc_process_data] Processing data for request: " + requestId);

        int records = 0;
        if (parsedData.get("records") != null) {
            records = ((Number) parsedData.get("records")).intValue();
        }

        int recordsFailed = 2;
        int recordsSucceeded = records - recordsFailed;

        Map<String, Object> processingResult = Map.of(
                "recordsProcessed", records,
                "recordsSucceeded", recordsSucceeded,
                "recordsFailed", recordsFailed,
                "processingTimeMs", 1847,
                "outputFormat", "normalized_json"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("requestId", requestId);
        result.getOutputData().put("result", processingResult);
        result.getOutputData().put("processingStatus", "completed");
        return result;
    }
}
