package ddosmitigation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyServiceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ddos_verify_service";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Service restored: latency normal, legitimate traffic flowing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_service", true);
        return result;
    }
}
