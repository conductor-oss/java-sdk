package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Logs normal activity when no anomalous patterns are detected.
 * Input: message (string)
 * Output: logged (true)
 */
public class LogNormalWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_log_normal";
    }

    @Override
    public TaskResult execute(Task task) {
        String message = (String) task.getInputData().get("message");
        if (message == null) {
            message = "No message provided";
        }

        System.out.println("  [cp_log_normal] " + message);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("logged", true);
        return result;
    }
}
