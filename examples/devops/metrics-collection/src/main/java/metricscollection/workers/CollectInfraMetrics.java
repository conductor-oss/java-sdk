package metricscollection.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Collects infrastructure-level metrics such as CPU, memory, and disk I/O.
 */
public class CollectInfraMetrics implements Worker {

    @Override
    public String getTaskDefName() {
        return "mc_collect_infra";
    }

    @Override
    public TaskResult execute(Task task) {
        String environment = (String) task.getInputData().get("environment");
        String source = (String) task.getInputData().get("source");

        System.out.println("[mc_collect_infra] Collecting infrastructure metrics from " + environment);

        TaskResult result = new TaskResult(task);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("metricCount", 32);

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("cpuUsage", 65);
        metrics.put("memoryUsage", 72);
        metrics.put("diskIO", 340);
        output.put("metrics", metrics);

        output.put("source", source != null ? source : "infrastructure");

        result.setStatus(TaskResult.Status.COMPLETED);
        result.setOutputData(output);
        return result;
    }
}
