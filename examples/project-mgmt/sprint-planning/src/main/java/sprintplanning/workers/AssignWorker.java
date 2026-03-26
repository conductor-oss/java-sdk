package sprintplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class AssignWorker implements Worker {
    @Override public String getTaskDefName() { return "spn_assign"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assign] Assigning stories to team members");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("assignments", List.of(Map.of("story","US-101","assignee","Alice"),Map.of("story","US-102","assignee","Bob"),Map.of("story","US-103","assignee","Carol"))); return r;
    }
}
