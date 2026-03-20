package publicrecords.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReleaseWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbr_release";
    }

    @Override
    public TaskResult execute(Task task) {
        String requesterId = (String) task.getInputData().get("requesterId");
        System.out.printf("  [release] 3 redacted documents released to %s%n", requesterId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("released", true);
        result.getOutputData().put("count", 3);
        return result;
    }
}
