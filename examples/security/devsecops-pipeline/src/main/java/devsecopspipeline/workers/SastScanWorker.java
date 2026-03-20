package devsecopspipeline.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SastScanWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dso_sast_scan";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [sast] Static analysis: 0 critical, 2 medium");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("sast_scanId", "SAST_SCAN-1356");
        result.addOutputData("success", true);
        return result;
    }
}
