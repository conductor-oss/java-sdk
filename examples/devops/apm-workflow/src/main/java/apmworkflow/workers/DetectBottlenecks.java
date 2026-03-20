package apmworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Detects performance bottlenecks based on latency analysis.
 */
public class DetectBottlenecks implements Worker {

    @Override
    public String getTaskDefName() {
        return "apm_detect_bottlenecks";
    }

    @Override
    public TaskResult execute(Task task) {
        Object p99Latency = task.getInputData().get("p99Latency");
        Object p50Latency = task.getInputData().get("p50Latency");

        System.out.println("[apm_detect_bottlenecks] Detecting bottlenecks (p99: " + p99Latency + "ms, p50: " + p50Latency + "ms)...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("bottleneckCount", 2);
        result.getOutputData().put("bottlenecks", List.of(
                Map.of("endpoint", "/api/search", "cause", "N+1 query", "impact", "high"),
                Map.of("endpoint", "/api/export", "cause", "large payload serialization", "impact", "medium")
        ));
        result.getOutputData().put("recommendations", List.of(
                "Add database index on search fields",
                "Implement streaming for export endpoint"
        ));
        return result;
    }
}
