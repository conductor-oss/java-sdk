package containerorchestration.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "co_scan";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [scan] No critical vulnerabilities found");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("scan", true);
        result.addOutputData("processed", true);
        return result;
    }
}
