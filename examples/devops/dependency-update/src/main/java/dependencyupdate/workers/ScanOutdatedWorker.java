package dependencyupdate.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScanOutdatedWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "du_scan_outdated";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [scan] auth-service: 8 outdated dependencies");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("scan_outdatedId", "SCAN_OUTDATED-1334");
        result.addOutputData("success", true);
        return result;
    }
}
