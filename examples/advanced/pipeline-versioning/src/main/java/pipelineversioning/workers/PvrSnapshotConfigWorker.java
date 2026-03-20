package pipelineversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PvrSnapshotConfigWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pvr_snapshot_config";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [snapshot] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("configSnapshot", java.util.Map.of("steps", java.util.List.of("extract","transform","validate","load"), "parallelism", 4));
        result.getOutputData().put("configHash", "sha256:" + Long.toHexString(System.currentTimeMillis()));
        result.getOutputData().put("stepCount", 4);
        return result;
    }
}