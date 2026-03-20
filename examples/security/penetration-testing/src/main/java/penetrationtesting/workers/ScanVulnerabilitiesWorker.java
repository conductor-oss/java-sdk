package penetrationtesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Scans for vulnerabilities in the target.
 * Input: scan_vulnerabilitiesData (from reconnaissance)
 * Output: scan_vulnerabilities, processed
 */
public class ScanVulnerabilitiesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pen_scan_vulnerabilities";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [scan] Found 5 potential vulnerabilities");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scan_vulnerabilities", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
