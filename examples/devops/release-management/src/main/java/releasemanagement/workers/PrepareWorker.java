package releasemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rm_prepare";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [prepare] Release 3.2.0 for platform: 12 changes, 3 fixes");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("prepareId", "PREPARE-1500");
        result.addOutputData("success", true);
        return result;
    }
}
