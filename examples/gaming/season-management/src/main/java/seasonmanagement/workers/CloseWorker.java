package seasonmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class CloseWorker implements Worker {
    @Override public String getTaskDefName() { return "smg_close"; }
    @Override public TaskResult execute(Task task) {
        String seasonId = (String) task.getInputData().get("seasonId");
        System.out.println("  [close] Season " + seasonId + " concluded");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("season", Map.of("seasonId", seasonId != null ? seasonId : "S3-2026", "totalPlayers", 25000, "rewardsDistributed", 18000, "status", "ENDED"));
        return r;
    }
}
