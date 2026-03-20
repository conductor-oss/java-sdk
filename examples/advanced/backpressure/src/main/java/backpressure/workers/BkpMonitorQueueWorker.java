package backpressure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class BkpMonitorQueueWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "bkp_monitor_queue";
    }

    @Override
    public TaskResult execute(Task task) {
        int high = task.getInputData().get("thresholdHigh") instanceof Number ? ((Number) task.getInputData().get("thresholdHigh")).intValue() : 500;
        int critical = task.getInputData().get("thresholdCritical") instanceof Number ? ((Number) task.getInputData().get("thresholdCritical")).intValue() : 1000;
        int queueDepth = 750;
        String pressureLevel = "ok"; int throttlePercent = 0; int shedPercent = 0;
        if (queueDepth >= critical) { pressureLevel = "critical"; shedPercent = 50; }
        else if (queueDepth >= high) { pressureLevel = "high"; throttlePercent = 40; }
        System.out.println("  [monitor] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("queueDepth", queueDepth);
        result.getOutputData().put("pressureLevel", pressureLevel);
        result.getOutputData().put("throttlePercent", throttlePercent);
        result.getOutputData().put("shedPercent", shedPercent);
        return result;
    }
}