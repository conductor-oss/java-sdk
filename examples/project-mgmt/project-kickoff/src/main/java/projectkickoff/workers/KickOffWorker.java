package projectkickoff.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class KickOffWorker implements Worker {
    @Override public String getTaskDefName() { return "pkf_kick_off"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [kickoff] Project " + task.getInputData().get("projectName") + " kicked off!");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("project", Map.of("name",task.getInputData().get("projectName"),"status","ACTIVE","kickoffDate","2026-03-08")); return r;
    }
}
