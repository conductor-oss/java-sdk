package gracefulshutdown.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GshCheckpointWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "gsh_checkpoint";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [checkpoint] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        String checkpointId = "chk-" + Long.toString(System.currentTimeMillis(), 36);
        result.getOutputData().put("checkpointId", checkpointId);
        return result;
    }
}