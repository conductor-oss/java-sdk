package performancetesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PrepareEnvWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pt_prepare_env";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [prepare] Provisioned load test environment");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("prepare_envId", "PREPARE_ENV-1430");
        result.addOutputData("success", true);
        return result;
    }
}
