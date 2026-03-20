package contractanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "cna_analyze"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [cna_analyze] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("overallRisk", "medium");
        result.getOutputData().put("parsed", Map.of("pages",42,"sections",18));
        result.getOutputData().put("clauses", Map.of("termination","90-day notice","liability","capped"));
        result.getOutputData().put("risks", java.util.List.of(Map.of("clause","nonCompete","risk","high")));
        return result;
    }
}
