package tenantscreening.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CreditCheckWorker implements Worker {
    @Override public String getTaskDefName() { return "tsc_credit"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [tsc_credit] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", "good");
        result.getOutputData().put("score", 700);
        return result;
    }
}
