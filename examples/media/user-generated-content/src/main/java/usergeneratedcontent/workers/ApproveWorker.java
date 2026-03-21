package usergeneratedcontent.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApproveWorker implements Worker {
    @Override public String getTaskDefName() { return "ugc_approve"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [approve] Processing " + task.getInputData().getOrDefault("approvalMethod", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("approvalMethod", "auto");
        r.getOutputData().put("approvedAt", "2026-03-08T09:01:00Z");
        return r;
    }
}
