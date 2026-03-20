package taxfiling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CalculateTaxWorker implements Worker {
    @Override public String getTaskDefName() { return "txf_calculate_tax"; }
    @Override public TaskResult execute(Task task) {
        double gross = toDouble(task.getInputData().get("grossIncome"));
        double deductions = toDouble(task.getInputData().get("deductions"));
        double credits = toDouble(task.getInputData().get("credits"));
        double taxable = gross - deductions;
        long taxLiability = Math.round(taxable * 0.22 - credits);
        String effectiveRate = String.format("%.1f", (taxLiability / gross) * 100);
        System.out.println("  [calculate] Taxable: $" + taxable + ", Liability: $" + taxLiability + ", Rate: " + effectiveRate + "%");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("taxLiability", taxLiability); r.getOutputData().put("taxableIncome", taxable);
        r.getOutputData().put("effectiveRate", effectiveRate);
        return r;
    }
    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; } }
}
