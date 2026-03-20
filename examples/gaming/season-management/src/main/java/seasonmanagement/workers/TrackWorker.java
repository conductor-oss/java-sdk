package seasonmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "smg_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking season " + task.getInputData().get("seasonId") + " progress");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("stats", Map.of("activePlayers", 25000, "avgLevel", 22, "topLevel", 50, "passHolders", 8500));
        return r;
    }
}
