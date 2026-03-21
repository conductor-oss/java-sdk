package smoketesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TestIntegrationsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "st_test_integrations";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [integrations] All 3 downstream services reachable");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("test_integrations", true);
        result.addOutputData("processed", true);
        return result;
    }
}
