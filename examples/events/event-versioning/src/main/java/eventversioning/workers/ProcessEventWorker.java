package eventversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Processes a versioned event after transformation.
 * Input: originalVersion, event
 * Output: processed (true), originalVersion
 */
public class ProcessEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vr_process_event";
    }

    @Override
    public TaskResult execute(Task task) {
        String originalVersion = (String) task.getInputData().get("originalVersion");
        if (originalVersion == null) {
            originalVersion = "unknown";
        }

        System.out.println("  [vr_process_event] Processing event (original version: " + originalVersion + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        result.getOutputData().put("originalVersion", originalVersion);
        return result;
    }
}
