package changerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "chr_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Change " + task.getInputData().get("changeId") + " approved");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approval", Map.of("approved",true,"approver","PM Lead","date","2026-03-08")); return r;
    }
}
