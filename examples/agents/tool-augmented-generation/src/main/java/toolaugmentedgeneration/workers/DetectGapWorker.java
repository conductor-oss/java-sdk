package toolaugmentedgeneration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Analyses partial text to detect the knowledge gap and determines which
 * external tool should be called and what query to send.
 */
public class DetectGapWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tg_detect_gap";
    }

    @Override
    public TaskResult execute(Task task) {
        String partialText = (String) task.getInputData().get("partialText");
        if (partialText == null || partialText.isBlank()) {
            partialText = "";
        }

        String gapType = (String) task.getInputData().get("gapType");
        if (gapType == null || gapType.isBlank()) {
            gapType = "unknown";
        }

        System.out.println("  [tg_detect_gap] Detecting gap in partial text (type=" + gapType + ")");

        String toolName = "version_lookup";
        String toolQuery = "Node.js current LTS version";
        String gapLocation = "end";

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("toolName", toolName);
        result.getOutputData().put("toolQuery", toolQuery);
        result.getOutputData().put("gapLocation", gapLocation);
        return result;
    }
}
