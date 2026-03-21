package projectkickoff.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class AssignTeamWorker implements Worker {
    @Override public String getTaskDefName() { return "pkf_assign_team"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [team] Assigning team for " + task.getInputData().get("projectName"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("team", Map.of("lead","Alice","members",List.of("Bob","Carol","Dave"),"size",4)); return r;
    }
}
