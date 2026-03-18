package csvprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Generates final output with summary statistics from transformed rows.
 * Input: transformedRows (list of maps), totalParsed, totalValid
 * Output: recordCount, summary, records
 */
public class GenerateOutputWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cv_generate_output";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> rows =
                (List<Map<String, Object>>) task.getInputData().get("transformedRows");
        if (rows == null) {
            rows = List.of();
        }

        Object totalParsedObj = task.getInputData().get("totalParsed");
        int totalParsed = toInt(totalParsedObj);

        Object totalValidObj = task.getInputData().get("totalValid");
        int totalValid = toInt(totalValidObj);

        double totalSalary = 0.0;
        for (Map<String, Object> r : rows) {
            Object salaryObj = r.get("salary");
            if (salaryObj instanceof Number) {
                totalSalary += ((Number) salaryObj).doubleValue();
            }
        }

        double avgSalary = rows.isEmpty() ? 0.0 : totalSalary / rows.size();
        String summary = String.format("Processed %d rows, %d valid, avg salary $%.2f",
                totalParsed, totalValid, avgSalary);

        System.out.println("  [output] " + summary);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recordCount", rows.size());
        result.getOutputData().put("summary", summary);
        result.getOutputData().put("records", rows);
        return result;
    }

    private int toInt(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).intValue();
        }
        if (obj instanceof String) {
            try {
                return Integer.parseInt((String) obj);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
