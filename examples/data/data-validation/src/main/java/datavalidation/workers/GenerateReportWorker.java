package datavalidation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates a validation summary report.
 * Input: totalRecords, requiredErrors, typeErrors, rangeErrors, validRecords
 * Output: summary, passRate
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "vd_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        int totalRecords = toInt(task.getInputData().get("totalRecords"));
        int requiredErrors = toInt(task.getInputData().get("requiredErrors"));
        int typeErrors = toInt(task.getInputData().get("typeErrors"));
        int rangeErrors = toInt(task.getInputData().get("rangeErrors"));
        int validRecords = toInt(task.getInputData().get("validRecords"));

        String summary = "Validation complete: " + validRecords + "/" + totalRecords
                + " records valid. Errors: required=" + requiredErrors
                + ", type=" + typeErrors + ", range=" + rangeErrors;

        String passRate;
        if (totalRecords > 0) {
            passRate = String.format("%.1f%%", (validRecords * 100.0) / totalRecords);
        } else {
            passRate = "0.0%";
        }

        System.out.println("  [vd_generate_report] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("passRate", passRate);
        return result;
    }

    private int toInt(Object value) {
        if (value instanceof Number num) {
            return num.intValue();
        }
        return 0;
    }
}
