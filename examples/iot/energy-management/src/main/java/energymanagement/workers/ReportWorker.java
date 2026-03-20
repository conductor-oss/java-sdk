package energymanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates an energy report.
 * Input: buildingId, consumption, savings
 * Output: reportId, generated
 */
public class ReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "erg_report";
    }

    @Override
    public TaskResult execute(Task task) {
        String savings = (String) task.getInputData().getOrDefault("savings", "0%");
        String reportId = "RPT-20240115";

        System.out.println("  [report] Generated report " + reportId + " — savings: " + savings);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("reportId", reportId);
        result.getOutputData().put("generated", true);
        return result;
    }
}
