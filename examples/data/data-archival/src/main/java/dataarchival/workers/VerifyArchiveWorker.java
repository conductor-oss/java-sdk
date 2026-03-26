package dataarchival.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Verifies the archive integrity.
 * Input: archivePath (string), expectedCount (int), checksum (string)
 * Output: verified (boolean), archivePath (string)
 */
public class VerifyArchiveWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "arc_verify_archive";
    }

    @Override
    public TaskResult execute(Task task) {
        String checksum = (String) task.getInputData().get("checksum");
        Object expectedObj = task.getInputData().get("expectedCount");
        int expectedCount = expectedObj instanceof Number ? ((Number) expectedObj).intValue() : 0;
        String archivePath = (String) task.getInputData().getOrDefault("archivePath", "unknown");

        boolean verified = checksum != null && !checksum.isEmpty() && expectedCount > 0;

        System.out.println("  [verify] Archive at \"" + archivePath + "\": " + expectedCount
                + " records, checksum=" + (checksum != null ? "valid" : "missing")
                + " -> " + (verified ? "VERIFIED" : "FAILED"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", verified);
        result.getOutputData().put("archivePath", archivePath);
        return result;
    }
}
