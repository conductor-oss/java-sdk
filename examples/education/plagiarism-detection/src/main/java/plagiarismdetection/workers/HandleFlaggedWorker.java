package plagiarismdetection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HandleFlaggedWorker implements Worker {
    @Override public String getTaskDefName() { return "plg_handle_flagged"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [flagged] " + task.getInputData().get("studentId") + ": " + task.getInputData().get("similarityScore") + "% match - escalated to instructor");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "escalated");
        result.getOutputData().put("notified", "instructor");
        return result;
    }
}
