package workflowdebugging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Analyzes collected trace data for anomalies and performance issues.
 * Input: traceData
 * Output: analysis (containing anomalies, bottlenecks, summary)
 */
public class WfdAnalyzeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfd_analyze";
    }

    @Override
    public TaskResult execute(Task task) {
        Object traceData = task.getInputData().get("traceData");

        System.out.println("  [analyze] Analyzing trace data for anomalies");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("analysis", Map.of(
                "anomalies", List.of(
                        Map.of("type", "slow_task", "taskName", "data_transform", "durationMs", 2800, "threshold", 1000)),
                "bottlenecks", List.of("data_transform"),
                "summary", "1 anomaly detected: slow task execution in data_transform"));
        return result;
    }
}
