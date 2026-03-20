package networksegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ConfigureRulesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_configure_rules";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [rules] Configured 28 firewall rules between zones");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("configure_rules", true);
        result.addOutputData("processed", true);
        return result;
    }
}
