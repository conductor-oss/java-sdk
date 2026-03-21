package workerscaling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class WksCalculateNeededWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wks_calculate_needed";
    }

    @Override
    public TaskResult execute(Task task) {
        int depth = task.getInputData().get("queueDepth") instanceof Number ? ((Number) task.getInputData().get("queueDepth")).intValue() : 0;
        int avgMs = task.getInputData().get("avgProcessingMs") instanceof Number ? ((Number) task.getInputData().get("avgProcessingMs")).intValue() : 200;
        int targetMs = task.getInputData().get("targetLatencyMs") instanceof Number ? ((Number) task.getInputData().get("targetLatencyMs")).intValue() : 5000;
        int needed = (int) Math.ceil((double)(depth * avgMs) / targetMs);
        System.out.println("  [calculate] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("desiredWorkers", needed);
        result.getOutputData().put("scaleFactor", String.format("%.2f", (double) needed / 5));
        return result;
    }
}