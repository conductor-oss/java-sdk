package investmentworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class MonitorWorker implements Worker {
    @Override public String getTaskDefName() { return "ivt_monitor"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Monitoring trade " + task.getInputData().get("tradeId") + " at $" + task.getInputData().get("executedPrice"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("monitoringStatus", "active"); r.getOutputData().put("stopLoss", 167.00);
        r.getOutputData().put("takeProfit", 210.00);
        r.getOutputData().put("alertsConfigured", List.of("price_drop_5pct","earnings_date","volume_spike"));
        return r;
    }
}
