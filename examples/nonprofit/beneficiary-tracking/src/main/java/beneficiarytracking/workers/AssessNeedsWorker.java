package beneficiarytracking.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AssessNeedsWorker implements Worker {
    @Override public String getTaskDefName() { return "btr_assess_needs"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [assess] Assessing needs for " + task.getInputData().get("beneficiaryId"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("needs", Map.of("housing", "stable", "food", "insecure", "education", "enrolled", "health", "needs-checkup")); return r;
    }
}
