package seoworkflow.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AuditSiteWorker implements Worker {
    @Override public String getTaskDefName() { return "seo_audit_site"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [audit] Processing " + task.getInputData().getOrDefault("seoScore", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("seoScore", 72);
        r.getOutputData().put("issues", List.of("missing_meta_descriptions"));
        r.getOutputData().put("currentRankings", Map.of());
        return r;
    }
}
