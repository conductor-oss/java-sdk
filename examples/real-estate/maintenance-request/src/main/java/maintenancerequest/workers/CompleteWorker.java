package maintenancerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CompleteWorker implements Worker {
    @Override public String getTaskDefName() { return "mtr_complete"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtr_complete] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("laborHours", "2.5");
        result.getOutputData().put("priority", "high");
        return result;
    }
}
