package offermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class GenerateWorker implements Worker {
    @Override public String getTaskDefName() { return "ofm_generate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [generate] Offer for " + task.getInputData().get("candidateName") + ": " + task.getInputData().get("position") + " at $" + task.getInputData().get("salary"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("offerId", "OFR-604");
        r.getOutputData().put("generated", true);
        return r;
    }
}
