package regulatoryreporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class SubmitWorker implements Worker {
    @Override public String getTaskDefName() { return "reg_submit"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [submit] Submitting " + task.getInputData().get("reportType") + " report to regulatory portal");
        result.getOutputData().put("submissionId", "REGFIL-2024-Q1-001");
        result.getOutputData().put("submittedAt", Instant.now().toString());
        result.getOutputData().put("deadline", "2024-04-30");
        result.getOutputData().put("submittedBefore", true);
        return result;
    }
}
