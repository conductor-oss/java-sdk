package investmentworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DecideWorker implements Worker {
    @Override public String getTaskDefName() { return "ivt_decide"; }
    @Override public TaskResult execute(Task task) {
        double maxInvest = toDouble(task.getInputData().get("maxInvestment"));
        int shares = (int) Math.floor(maxInvest / 185.50);
        System.out.println("  [decide] Recommendation: " + task.getInputData().get("recommendation") + ", allocating " + shares + " shares");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("action", "buy"); r.getOutputData().put("shares", shares);
        r.getOutputData().put("priceLimit", 190.00); r.getOutputData().put("orderType", "limit");
        return r;
    }
    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(String.valueOf(o)); } catch (Exception e) { return 0; } }
}
