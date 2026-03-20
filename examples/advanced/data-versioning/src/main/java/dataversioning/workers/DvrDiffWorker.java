package dataversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DvrDiffWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dvr_diff";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [diff] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", "+15230 ~3421 -892");
        result.getOutputData().put("diffStats", java.util.Map.of("added", 15230, "modified", 3421, "deleted", 892));
        result.getOutputData().put("needsRollback", false);
        return result;
    }
}