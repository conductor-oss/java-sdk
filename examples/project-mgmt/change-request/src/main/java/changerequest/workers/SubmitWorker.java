package changerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "chr_submit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [submit] Change request " + task.getInputData().get("changeId") + ": " + task.getInputData().get("description"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("details", Map.of("id",String.valueOf(task.getInputData().get("changeId")),"type","scope_change","submittedAt","2026-03-08")); return r;
    }
}
