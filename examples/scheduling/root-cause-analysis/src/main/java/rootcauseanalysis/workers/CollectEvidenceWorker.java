package rootcauseanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CollectEvidenceWorker implements Worker {
    @Override public String getTaskDefName() { return "rca_collect_evidence"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [evidence] Collecting evidence");
        r.getOutputData().put("logCount", 2400);
        r.getOutputData().put("metricCount", 150);
        r.getOutputData().put("recentChanges", List.of(Map.of("service","auth-service","type","deployment")));
        r.getOutputData().put("totalEvidence", 2551);
        return r;
    }
}
