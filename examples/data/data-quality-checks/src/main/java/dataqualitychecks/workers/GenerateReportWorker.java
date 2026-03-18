package dataqualitychecks.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Generates a quality report from the three check scores.
 * Input: completeness (double), accuracy (double), consistency (double), totalRecords (int)
 * Output: overallScore (double), grade (string), totalRecords (int)
 */
public class GenerateReportWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "qc_generate_report";
    }

    @Override
    public TaskResult execute(Task task) {
        double completeness = toDouble(task.getInputData().get("completeness"));
        double accuracy = toDouble(task.getInputData().get("accuracy"));
        double consistency = toDouble(task.getInputData().get("consistency"));
        Object totalRecordsObj = task.getInputData().get("totalRecords");

        double overall = Math.round(((completeness + accuracy + consistency) / 3.0) * 100.0) / 100.0;
        String grade;
        if (overall >= 0.9) grade = "A";
        else if (overall >= 0.8) grade = "B";
        else if (overall >= 0.7) grade = "C";
        else grade = "D";

        System.out.println("  [report] Quality Report: completeness=" + Math.round(completeness * 100)
                + "%, accuracy=" + Math.round(accuracy * 100)
                + "%, consistency=" + Math.round(consistency * 100)
                + "% -> overall=" + Math.round(overall * 100) + "% (" + grade + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallScore", overall);
        result.getOutputData().put("grade", grade);
        result.getOutputData().put("totalRecords", totalRecordsObj);
        return result;
    }

    private double toDouble(Object obj) {
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        if (obj instanceof String) {
            try { return Double.parseDouble((String) obj); } catch (NumberFormatException e) { return 0.0; }
        }
        return 0.0;
    }
}
