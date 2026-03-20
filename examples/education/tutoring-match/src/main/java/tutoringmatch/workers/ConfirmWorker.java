package tutoringmatch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "tut_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Session " + task.getInputData().get("sessionId") + " confirmed - both parties notified");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("confirmed", true);
        result.getOutputData().put("reminderSet", true);
        return result;
    }
}
