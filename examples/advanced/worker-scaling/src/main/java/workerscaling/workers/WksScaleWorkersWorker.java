package workerscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WksScaleWorkersWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wks_scale_workers";
    }

    @Override
    public TaskResult execute(Task task) {
        int current = task.getInputData().get("currentWorkers") instanceof Number ? ((Number) task.getInputData().get("currentWorkers")).intValue() : 5;
        int desired = task.getInputData().get("desiredWorkers") instanceof Number ? ((Number) task.getInputData().get("desiredWorkers")).intValue() : 5;
        String action = desired > current ? "scale_up" : desired < current ? "scale_down" : "no_change";
        System.out.println("  [scale] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("scalingAction", action);
        result.getOutputData().put("newWorkerCount", desired);
        result.getOutputData().put("previousCount", current);
        return result;
    }
}