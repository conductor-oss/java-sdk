package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BuildWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_build";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [build] Built user-service:v2.1.0");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("buildId", "BUILD-1333");
        result.addOutputData("success", true);
        return result;
    }
}
