package workflowmigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WmVerifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wm_verify";
    }

    @Override
    public TaskResult execute(Task task) {
        int orig = task.getInputData().get("originalTasks") instanceof Number ? ((Number) task.getInputData().get("originalTasks")).intValue() : 0;
        int imported = task.getInputData().get("importedTasks") instanceof Number ? ((Number) task.getInputData().get("importedTasks")).intValue() : 0;
        System.out.println("  [verify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verified", orig == imported);
        result.getOutputData().put("taskDelta", imported - orig);
        return result;
    }
}