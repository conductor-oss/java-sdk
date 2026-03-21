package dataversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DvrSnapshotWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dvr_snapshot";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [snapshot] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("snapshotId", "SNAP-" + System.currentTimeMillis());
        result.getOutputData().put("rows", 1250000);
        result.getOutputData().put("checksum", "sha256:abc123");
        return result;
    }
}