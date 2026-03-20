package offermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AcceptWorker implements Worker {
    @Override public String getTaskDefName() { return "ofm_accept"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [accept] " + task.getInputData().get("candidateName") + " accepted the offer!");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("accepted", true);
        r.getOutputData().put("startDate", "2024-05-01");
        return r;
    }
}
