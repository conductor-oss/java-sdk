package payrollworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ApplyDeductionsWorker implements Worker {
    @Override public String getTaskDefName() { return "prl_apply_deductions"; }
    @Override public TaskResult execute(Task task) {
        double gross = toDouble(task.getInputData().get("grossPayroll"));
        double federalTax = Math.round(gross * 0.22 * 100.0) / 100.0;
        double stateTax = Math.round(gross * 0.05 * 100.0) / 100.0;
        double benefits = Math.round(gross * 0.08 * 100.0) / 100.0;
        double total = federalTax + stateTax + benefits;
        double net = Math.round((gross - total) * 100.0) / 100.0;
        System.out.println("  [deductions] Federal: $" + federalTax + ", State: $" + stateTax + ", Benefits: $" + benefits);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("netPayroll", net); r.getOutputData().put("totalDeductions", total);
        r.getOutputData().put("federalTax", federalTax); r.getOutputData().put("stateTax", stateTax); r.getOutputData().put("benefits", benefits);
        return r;
    }
    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; } }
}
