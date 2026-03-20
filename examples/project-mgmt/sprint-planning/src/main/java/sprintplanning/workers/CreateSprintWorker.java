package sprintplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CreateSprintWorker implements Worker {
    @Override public String getTaskDefName() { return "spn_create_sprint"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [sprint] Created Sprint " + task.getInputData().get("sprintNumber"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("sprint", Map.of("number",task.getInputData().get("sprintNumber"),"stories",3,"totalPoints",16,"status","ACTIVE")); return r;
    }
}
