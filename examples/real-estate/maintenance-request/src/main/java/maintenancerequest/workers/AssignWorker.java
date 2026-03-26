package maintenancerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class AssignWorker implements Worker {
    @Override public String getTaskDefName() { return "mtr_assign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtr_assign] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("technicianId", "TECH-12");
        result.getOutputData().put("priority", "high");
        return result;
    }
}
