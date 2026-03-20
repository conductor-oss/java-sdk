package apmworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Analyzes latency percentiles from collected traces.
 */
public class AnalyzeLatency implements Worker {

    @Override
    public String getTaskDefName() {
        return "apm_analyze_latency";
    }

    @Override
    public TaskResult execute(Task task) {
        Object traceCount = task.getInputData().get("traceCount");
        String percentile = (String) task.getInputData().get("percentile");

        System.out.println("[apm_analyze_latency] Analyzing latency from " + traceCount + " traces (" + percentile + " percentile)...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("p50Latency", 45);
        result.getOutputData().put("p95Latency", 180);
        result.getOutputData().put("p99Latency", 520);
        result.getOutputData().put("meanLatency", 62);
        result.getOutputData().put("slowEndpoints", List.of("/api/search", "/api/export"));
        return result;
    }
}
