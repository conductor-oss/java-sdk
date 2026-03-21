package backgroundcheck.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ConsentWorker implements Worker {
    @Override public String getTaskDefName() { return "bgc_consent"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [consent] " + task.getInputData().get("candidateName") + " provided consent for background check");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("consented", true);
        r.getOutputData().put("consentDate", "2024-03-10");
        return r;
    }
}
