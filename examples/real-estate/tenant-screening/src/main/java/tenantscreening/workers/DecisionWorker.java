package tenantscreening.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class DecisionWorker implements Worker {
    @Override public String getTaskDefName() { return "tsc_decision"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [tsc_decision] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", "approved");
        result.getOutputData().put("score", 700);
        return result;
    }
}
