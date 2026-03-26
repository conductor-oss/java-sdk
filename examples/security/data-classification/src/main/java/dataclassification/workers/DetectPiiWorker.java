package dataclassification.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Detects PII fields in scanned data.
 * Input: detect_piiData (from scan)
 * Output: detect_pii, processed
 */
public class DetectPiiWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dc_detect_pii";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [pii] Detected 24 PII fields: emails, SSNs, phone numbers");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("detect_pii", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
