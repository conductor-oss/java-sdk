package rootcauseanalysis.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DetectIssueWorker implements Worker {
    @Override public String getTaskDefName() { return "rca_detect_issue"; }
    @Override public TaskResult execute(Task task) {
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [detect] Incident " + task.getInputData().get("incidentId") + ": " + task.getInputData().get("symptom"));
        r.getOutputData().put("timeWindow", "2026-03-08T05:30:00Z/2026-03-08T06:00:00Z");
        r.getOutputData().put("relatedServices", List.of("api-gateway","auth-service","database"));
        r.getOutputData().put("impactLevel", "high");
        return r;
    }
}
