package apmworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

/**
 * Generates the final APM report summarizing bottlenecks and recommendations.
 */
public class ApmReport implements Worker {

    @Override
    public String getTaskDefName() {
        return "apm_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String serviceName = (String) task.getInputData().get("serviceName");

        System.out.println("[apm_report] Generating APM report for " + serviceName + "...");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportGenerated", true);
        result.getOutputData().put("reportUrl", "https://apm.example.com/report/checkout-2026-03-08");
        result.getOutputData().put("generatedAt", "2026-03-08T06:10:00Z");
        return result;
    }
}
