package offermanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DeclineWorker implements Worker {
    @Override public String getTaskDefName() { return "ofm_decline"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [decline] " + task.getInputData().get("candidateName") + " declined the offer");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("declined", true);
        r.getOutputData().put("reason", "counter-offer");
        return r;
    }
}
