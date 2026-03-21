package workflowdebugging.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Generates a debug report from the analysis results.
 * Input: analysis, workflowName
 * Output: debugReport
 */
public class WfdReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wfd_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String workflowName = (String) task.getInputData().getOrDefault("workflowName", "unknown");
        Object analysis = task.getInputData().get("analysis");

        System.out.println("  [report] Generating debug report for workflow \"" + workflowName + "\"");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("debugReport", Map.of(
                "workflowName", workflowName,
                "generatedAt", System.currentTimeMillis(),
                "status", "completed",
                "findings", "1 anomaly detected",
                "recommendation", "Investigate slow task 'data_transform' - execution exceeded threshold by 1800ms"));
        return result;
    }
}
