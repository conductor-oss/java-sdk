package regulatoryreporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public class ConfirmWorker implements Worker {
    @Override public String getTaskDefName() { return "reg_confirm"; }

    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        System.out.println("  [confirm] Submission " + task.getInputData().get("submissionId") + " confirmed by regulator");
        result.getOutputData().put("accepted", true);
        result.getOutputData().put("receiptNumber", "RCT-FED-88201");
        result.getOutputData().put("confirmedAt", Instant.now().toString());
        return result;
    }
}
