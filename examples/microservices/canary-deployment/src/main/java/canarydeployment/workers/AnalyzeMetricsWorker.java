package canarydeployment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AnalyzeMetricsWorker implements Worker {

    @Override public String getTaskDefName() { return "cd_analyze_metrics"; }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");
        if (serviceName == null) serviceName = "unknown-service";
        String duration = (String) task.getInputData().get("duration");
        if (duration == null) duration = "5m";

        System.out.println("  [analyze] Error rate: 0.3%, p99 latency: 120ms (duration=" + duration + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("errorRate", 0.3);
        result.getOutputData().put("p99Latency", 120);
        result.getOutputData().put("healthy", true);
        result.getOutputData().put("duration", duration);
        return result;
    }
}
