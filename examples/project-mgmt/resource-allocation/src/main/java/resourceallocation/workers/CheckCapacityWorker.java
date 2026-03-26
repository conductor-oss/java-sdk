package resourceallocation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class CheckCapacityWorker implements Worker {
    @Override public String getTaskDefName() { return "ral_check_capacity"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [capacity] Checking " + task.getInputData().get("resourceType") + " availability");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("available", List.of(Map.of("name","Alice","freeHours",30),Map.of("name","Bob","freeHours",20))); return r;
    }
}
