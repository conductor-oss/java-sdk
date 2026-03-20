package cryptocurrencytrading.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class MonitorMarketWorker implements Worker {
    @Override public String getTaskDefName() { return "cry_monitor_market"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [monitor] Monitoring " + task.getInputData().get("pair"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("currentPrice", 67432.50); r.getOutputData().put("change24h", 2.3);
        r.getOutputData().put("volume24h", 28500000000L);
        r.getOutputData().put("marketData", Map.of("rsi",62,"macd",150,"sma50",65000,"sma200",58000));
        return r;
    }
}
