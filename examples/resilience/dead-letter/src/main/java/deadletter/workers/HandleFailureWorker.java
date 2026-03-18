package deadletter.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Worker for dl_handle_failure -- handles dead-letter entries by writing
 * failure details to a real file and tracking them in memory for retry.
 *
 * Takes failedWorkflowId, failedTaskName, and error as inputs.
 * Returns handled=true with a summary of the failure.
 */
public class HandleFailureWorker implements Worker {

    // Track all handled failures for retry logic
    static final ConcurrentHashMap<String, String> HANDLED_FAILURES = new ConcurrentHashMap<>();
    private static Path deadLetterFile;

    @Override
    public String getTaskDefName() {
        return "dl_handle_failure";
    }

    @Override
    public TaskResult execute(Task task) {
        String failedWorkflowId = "";
        Object wfIdInput = task.getInputData().get("failedWorkflowId");
        if (wfIdInput instanceof String) {
            failedWorkflowId = (String) wfIdInput;
        }

        String failedTaskName = "";
        Object taskNameInput = task.getInputData().get("failedTaskName");
        if (taskNameInput instanceof String) {
            failedTaskName = (String) taskNameInput;
        }

        String error = "";
        Object errorInput = task.getInputData().get("error");
        if (errorInput instanceof String) {
            error = (String) errorInput;
        }

        // Write failure to dead letter file
        String entry = String.format("[%s] workflow=%s task=%s error=%s%n",
                Instant.now(), failedWorkflowId, failedTaskName, error);
        boolean writtenToFile = writeToDeadLetterFile(entry);

        // Track in memory for retry
        String key = failedWorkflowId + ":" + failedTaskName;
        HANDLED_FAILURES.put(key, error);

        String summary = "Failure handled for workflow "
                + failedWorkflowId + ", task " + failedTaskName + ": " + error;

        System.out.println("  [dl_handle_failure] " + summary);
        System.out.println("    Written to file: " + writtenToFile);
        System.out.println("    Total tracked failures: " + HANDLED_FAILURES.size());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("handled", true);
        result.getOutputData().put("failedWorkflowId", failedWorkflowId);
        result.getOutputData().put("failedTaskName", failedTaskName);
        result.getOutputData().put("error", error);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("writtenToFile", writtenToFile);
        result.getOutputData().put("totalTrackedFailures", HANDLED_FAILURES.size());
        return result;
    }

    private synchronized boolean writeToDeadLetterFile(String entry) {
        try {
            if (deadLetterFile == null) {
                deadLetterFile = Files.createTempFile("dead-letter-", ".log");
            }
            Files.writeString(deadLetterFile, entry, StandardOpenOption.APPEND);
            return true;
        } catch (IOException e) {
            System.out.println("  [dl_handle_failure] Failed to write to dead letter file: " + e.getMessage());
            return false;
        }
    }

    /** Clear state for testing. */
    static void clearState() {
        HANDLED_FAILURES.clear();
        deadLetterFile = null;
    }
}
