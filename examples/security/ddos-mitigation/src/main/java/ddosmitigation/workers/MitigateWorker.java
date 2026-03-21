package ddosmitigation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MitigateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ddos_mitigate";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [mitigate] Rate limiting enabled, challenge page activated, 1,200 IPs blocked");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("mitigate", true);
        result.addOutputData("processed", true);
        return result;
    }
}
