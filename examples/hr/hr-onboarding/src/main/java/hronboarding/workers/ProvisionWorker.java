package hronboarding.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Provisions systems (laptop, email, Slack, Jira) for a new employee.
 */
public class ProvisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "hro_provision";
    }

    @Override
    public TaskResult execute(Task task) {
        String employeeId = (String) task.getInputData().get("employeeId");

        System.out.println("  [provision] Provisioned laptop, email, Slack, Jira for " + employeeId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("systems", List.of("email", "slack", "jira", "github"));
        result.getOutputData().put("laptop", "MacBook Pro");
        return result;
    }
}
