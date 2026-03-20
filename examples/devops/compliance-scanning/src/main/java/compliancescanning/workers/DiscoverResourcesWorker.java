package compliancescanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DiscoverResourcesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_discover_resources";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [discover] Found 156 resources in production");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("discover_resourcesId", "DISCOVER_RESOURCES-1480");
        result.addOutputData("success", true);
        return result;
    }
}
