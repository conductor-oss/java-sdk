package passingoutput.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Generates a report with metrics and top products for a given region and period.
 * Outputs structured data (nested objects and arrays) that downstream tasks consume.
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String region = (String) task.getInputData().get("region");
        String period = (String) task.getInputData().get("period");

        System.out.println("  [report] Generating for " + region + " (" + period + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);

        result.getOutputData().put("metrics", Map.of(
                "revenue", 125000,
                "orders", 3400,
                "avgOrderValue", 36.76,
                "returnRate", 0.03
        ));

        result.getOutputData().put("topProducts", List.of(
                Map.of("name", "Widget A", "sales", 1200),
                Map.of("name", "Widget B", "sales", 890),
                Map.of("name", "Widget C", "sales", 650)
        ));

        result.getOutputData().put("generatedAt", Instant.now().toString());

        return result;
    }
}
