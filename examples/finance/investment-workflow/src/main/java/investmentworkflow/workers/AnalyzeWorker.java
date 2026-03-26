package investmentworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "ivt_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [analyze] Analyzing risk/return for " + task.getInputData().get("tickerSymbol"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("riskScore", 6.2); r.getOutputData().put("expectedReturn", 12.5);
        r.getOutputData().put("recommendation", "buy"); r.getOutputData().put("sharpeRatio", 1.45);
        return r;
    }
}
