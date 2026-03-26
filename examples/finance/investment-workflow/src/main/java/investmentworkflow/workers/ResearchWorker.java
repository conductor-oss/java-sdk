package investmentworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ResearchWorker implements Worker {
    @Override public String getTaskDefName() { return "ivt_research"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [research] Researching " + task.getInputData().get("tickerSymbol"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("fundamentals", Map.of("peRatio",22.5,"revenue","45B","earningsGrowth",0.15,"dividendYield",0.018));
        r.getOutputData().put("marketData", Map.of("price",185.50,"volume",12500000,"beta",1.12,"sma200",172.30));
        r.getOutputData().put("sector", "Technology");
        return r;
    }
}
