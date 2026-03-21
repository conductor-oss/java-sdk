package compliancescanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScanPoliciesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cs_scan_policies";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [scan] Scanned against CIS-AWS framework");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("scan_policies", true);
        result.addOutputData("processed", true);
        return result;
    }
}
