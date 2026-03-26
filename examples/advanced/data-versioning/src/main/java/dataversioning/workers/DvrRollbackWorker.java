package dataversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DvrRollbackWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dvr_rollback";
    }

    @Override
    public TaskResult execute(Task task) {
        Object nr = task.getInputData().get("needsRollback");
        boolean needs = Boolean.TRUE.equals(nr) || "true".equals(String.valueOf(nr));
        System.out.println("  [rollback] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rolledBack", needs);
        return result;
    }
}