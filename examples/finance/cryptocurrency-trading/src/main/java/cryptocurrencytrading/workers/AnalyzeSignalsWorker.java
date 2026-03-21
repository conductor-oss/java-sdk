package cryptocurrencytrading.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AnalyzeSignalsWorker implements Worker {
    @Override public String getTaskDefName() { return "cry_analyze_signals"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [signals] Analyzing signals for " + task.getInputData().get("pair") + " at $" + task.getInputData().get("currentPrice"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("signal", "buy"); r.getOutputData().put("confidence", 0.78);
        r.getOutputData().put("suggestedAmount", 0.15); r.getOutputData().put("holdReason", null);
        r.getOutputData().put("indicators", Map.of("trend","bullish","momentum","positive","support",64000));
        return r;
    }
}
