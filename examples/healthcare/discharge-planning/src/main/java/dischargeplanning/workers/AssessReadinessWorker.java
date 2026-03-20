package dischargeplanning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Evaluates discharge readiness for a patient.
 * Input: patientId, admissionId, diagnosis
 * Output: readiness, needs, lengthOfStay
 */
public class AssessReadinessWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dsc_assess_readiness";
    }

    @Override
    public TaskResult execute(Task task) {
        String patientId = (String) task.getInputData().get("patientId");
        if (patientId == null) patientId = "unknown";

        System.out.println("  [assess] Evaluating discharge readiness for " + patientId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("readiness", "ready");
        result.getOutputData().put("needs", List.of("home health aide", "medication reconciliation", "PT follow-up"));
        result.getOutputData().put("lengthOfStay", 4);
        return result;
    }
}
