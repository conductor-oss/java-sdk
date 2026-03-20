package disasterrecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class UpdateDnsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dr_update_dns";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [dns] DNS records updated");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("updated", true);
        return result;
    }
}
