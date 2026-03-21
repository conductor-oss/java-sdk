package payrollworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class CalculateGrossWorker implements Worker {
    @Override public String getTaskDefName() { return "prl_calculate_gross"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        List<Map<String,Object>> records = (List<Map<String,Object>>) task.getInputData().get("employeeRecords");
        if (records == null) records = List.of();
        double gross = 0;
        for (Map<String,Object> rec : records) {
            double regular = ((Number)rec.get("regularHours")).doubleValue();
            double ot = ((Number)rec.get("overtimeHours")).doubleValue();
            double rate = ((Number)rec.get("rate")).doubleValue();
            gross += (regular * rate) + (ot * rate * 1.5);
        }
        System.out.println("  [gross] Gross payroll for " + records.size() + " employees: $" + String.format("%.2f", gross));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("grossPayroll", String.format("%.2f", gross)); r.getOutputData().put("employeeCount", records.size());
        return r;
    }
}
