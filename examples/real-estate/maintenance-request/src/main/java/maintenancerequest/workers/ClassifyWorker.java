package maintenancerequest.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "mtr_classify"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [mtr_classify] Executing");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("category", "plumbing");
        result.getOutputData().put("priority", "high");
        return result;
    }
}
