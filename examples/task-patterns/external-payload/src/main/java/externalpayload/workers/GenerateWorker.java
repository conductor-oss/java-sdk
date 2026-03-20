package externalpayload.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Generates a summary and storage reference for a large payload.
 *
 * Instead of returning the full data (which could exceed payload limits),
 * this worker returns a compact summary and a storage reference (e.g., S3 URI)
 * that downstream tasks can use to retrieve the data if needed.
 *
 * Input:  { dataSize: number }
 * Output: { summary: { recordCount, avgSize, totalBytes }, storageRef: "s3://..." }
 */
public class GenerateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        Object dataSizeObj = task.getInputData().get("dataSize");
        int dataSize;
        if (dataSizeObj instanceof Number) {
            dataSize = ((Number) dataSizeObj).intValue();
        } else {
            dataSize = 1000;
        }

        if (dataSize <= 0) {
            dataSize = 1000;
        }

        System.out.println("  [ep_generate] Generating summary for " + dataSize + " records");

        int avgSize = 1024;
        long totalBytes = (long) dataSize * avgSize;

        Map<String, Object> summary = Map.of(
                "recordCount", dataSize,
                "avgSize", avgSize,
                "totalBytes", totalBytes
        );

        String storageRef = "s3://payloads/data.json";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("storageRef", storageRef);
        return result;
    }
}
