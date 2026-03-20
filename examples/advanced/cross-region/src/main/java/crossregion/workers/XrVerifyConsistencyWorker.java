package crossregion.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class XrVerifyConsistencyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "xr_verify_consistency";
    }

    @Override
    public TaskResult execute(Task task) {
        String pc = (String) task.getInputData().getOrDefault("primaryChecksum", "");
        String rc = (String) task.getInputData().getOrDefault("replicaChecksum", "");
        boolean match = pc.equals(rc);
        System.out.println("  [verify] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("consistent", match);
        return result;
    }
}