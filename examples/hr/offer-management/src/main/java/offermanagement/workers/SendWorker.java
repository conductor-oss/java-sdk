package offermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class SendWorker implements Worker {
    @Override public String getTaskDefName() { return "ofm_send"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [send] Offer letter sent to " + task.getInputData().get("candidateName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sent", true);
        r.getOutputData().put("expiresIn", "5 business days");
        return r;
    }
}
