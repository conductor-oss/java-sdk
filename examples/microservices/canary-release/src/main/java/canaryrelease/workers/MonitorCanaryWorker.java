package canaryrelease.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class MonitorCanaryWorker implements Worker {
    @Override public String getTaskDefName() { return "cy_monitor_canary"; }

    @Override public TaskResult execute(Task task) {
        Object pct = task.getInputData().getOrDefault("trafficPercent", 0);
        String dur = (String) task.getInputData().getOrDefault("monitorDuration", "5m");
        System.out.println("  [monitor] Monitoring at " + pct + "% for " + dur + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("errorRate", 0.02);
        result.getOutputData().put("p99Latency", "48ms");
        result.getOutputData().put("trafficPercent", pct);
        result.getOutputData().put("anomaliesDetected", 0);
        result.getOutputData().put("healthy", true);
        return result;
    }
}
