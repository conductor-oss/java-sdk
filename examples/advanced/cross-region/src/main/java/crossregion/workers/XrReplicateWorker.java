package crossregion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class XrReplicateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xr_replicate";
    }

    @Override
    public TaskResult execute(Task task) {
        String primary = (String) task.getInputData().getOrDefault("primaryRegion", "unknown");
        String replica = (String) task.getInputData().getOrDefault("replicaRegion", "unknown");
        System.out.println("  [replicate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("replicated", true);
        result.getOutputData().put("replicationId", "REP-" + System.currentTimeMillis());
        result.getOutputData().put("bytesTransferred", 52428800);
        return result;
    }
}