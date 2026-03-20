package virtualeconomy.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "vec_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Economy report for " + task.getInputData().get("playerId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("summary", Map.of("playerId", task.getInputData().getOrDefault("playerId","P-042"), "transactionId", task.getInputData().getOrDefault("transactionId","VTXN-750"), "status", "SETTLED"));
        return r;
    }
}
