package resourceallocation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AllocateWorker implements Worker {
    @Override public String getTaskDefName() { return "ral_allocate"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [allocate] Allocating resources to " + task.getInputData().get("projectId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("allocation", Map.of("resource","Alice","hours",30,"project",String.valueOf(task.getInputData().get("projectId")))); return r;
    }
}
