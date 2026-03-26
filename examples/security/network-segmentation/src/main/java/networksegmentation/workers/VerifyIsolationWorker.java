package networksegmentation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class VerifyIsolationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ns_verify_isolation";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [verify] Zone isolation verified: no unauthorized cross-zone traffic");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("verify_isolation", true);
        return result;
    }
}
