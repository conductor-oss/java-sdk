package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HandleCleanWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_handle_clean"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [clean] " + task.getInputData().get("assignmentId") + ": original work confirmed");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "approved");
        return result;
    }
}
