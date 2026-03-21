package beneficiarytracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ReportWorker implements Worker {
    @Override public String getTaskDefName() { return "btr_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Generating report for " + task.getInputData().get("beneficiaryId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("tracking", Map.of("beneficiaryId", task.getInputData().getOrDefault("beneficiaryId","BEN-758"), "outcomes", task.getInputData().getOrDefault("outcomes", Map.of()), "status", "PROGRESSING")); return r;
    }
}
