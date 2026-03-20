package usermigration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class LoadWorker implements Worker {
    @Override public String getTaskDefName() { return "umg_load"; }
    @Override public TaskResult execute(Task task) {
        String targetDb = (String) task.getInputData().get("targetDb");
        System.out.println("  [load] Loaded 500 users into " + targetDb);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("loadedCount", 500);
        result.getOutputData().put("failedCount", 0);
        return result;
    }
}
