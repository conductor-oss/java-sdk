package scholarshipprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ApplyWorker implements Worker {
    @Override public String getTaskDefName() { return "scp_apply"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [apply] " + task.getInputData().get("studentId") + " applying for " + task.getInputData().get("scholarshipId"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("applicationId", "SAPP-680-001");
        return result;
    }
}
