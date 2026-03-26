package usageanalytics.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "uag_report";
    }

    @Override
    public TaskResult execute(Task task) {

        String region = (String) task.getInputData().get("region");
        System.out.printf("  [report] Analytics report generated for %s%n", region);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", "RPT-usage-analytics-001");
        result.getOutputData().put("generated", true);
        return result;
    }
}
