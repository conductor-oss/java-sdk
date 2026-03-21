package codereviewai.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "cra_report"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        int sec = ((List<?>) task.getInputData().getOrDefault("security", List.of())).size();
        int qual = ((List<?>) task.getInputData().getOrDefault("quality", List.of())).size();
        int style = ((List<?>) task.getInputData().getOrDefault("style", List.of())).size();
        int total = sec + qual + style;
        String verdict = sec > 0 ? "CHANGES_REQUESTED" : total > 3 ? "COMMENT" : "APPROVED";
        System.out.println("  [report] Review complete: " + total + " findings, verdict: " + verdict);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("totalFindings", total); r.getOutputData().put("verdict", verdict);
        r.getOutputData().put("commentPosted", true); r.getOutputData().put("prUrl", task.getInputData().get("prUrl"));
        return r;
    }
}
