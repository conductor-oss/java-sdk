package qualityinspection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HandlePassWorker implements Worker {
    @Override public String getTaskDefName() { return "qi_handle_pass"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [pass] Batch " + task.getInputData().get("batchId") + " approved for release");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "release");
        result.getOutputData().put("approved", true);
        return result;
    }
}
