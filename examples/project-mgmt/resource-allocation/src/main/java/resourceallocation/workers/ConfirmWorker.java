package resourceallocation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "ral_confirm"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [confirm] Allocation confirmed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("confirmed", Map.of("resource","Alice","hours",30,"status","CONFIRMED")); return r;
    }
}
