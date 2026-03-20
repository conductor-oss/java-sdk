package crossregion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class XrSyncWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xr_sync";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [sync] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String checksum = "sha256:a1b2c3d4e5f6";
        result.getOutputData().put("synced", true);
        result.getOutputData().put("primaryChecksum", checksum);
        result.getOutputData().put("replicaChecksum", checksum);
        result.getOutputData().put("lagMs", 45);
        return result;
    }
}