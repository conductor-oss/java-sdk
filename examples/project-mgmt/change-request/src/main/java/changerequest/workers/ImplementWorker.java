package changerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ImplementWorker implements Worker {
    @Override public String getTaskDefName() { return "chr_implement"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [implement] Implementing change " + task.getInputData().get("changeId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", Map.of("implemented",true,"completedAt","2026-03-10")); return r;
    }
}
