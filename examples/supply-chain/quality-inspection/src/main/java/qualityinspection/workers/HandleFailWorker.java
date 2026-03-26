package qualityinspection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class HandleFailWorker implements Worker {
    @Override public String getTaskDefName() { return "qi_handle_fail"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [fail] Batch " + task.getInputData().get("batchId") + " quarantined — " +
                task.getInputData().get("defects") + " defects");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", "quarantine");
        result.getOutputData().put("approved", false);
        return result;
    }
}
