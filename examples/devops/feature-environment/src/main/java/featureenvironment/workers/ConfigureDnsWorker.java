package featureenvironment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfigureDnsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "fe_configure_dns";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dns] Preview URL configured");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("configure_dns", true);
        result.addOutputData("processed", true);
        return result;
    }
}
