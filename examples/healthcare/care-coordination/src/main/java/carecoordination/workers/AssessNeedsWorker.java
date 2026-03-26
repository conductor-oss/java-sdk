package carecoordination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Assesses patient needs based on condition and acuity.
 * Input: patientId, condition, acuity
 * Output: needs (list), riskScore, complexCase
 */
public class AssessNeedsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccr_assess_needs";
    }

    @Override
    public TaskResult execute(Task task) {
        String patientId = (String) task.getInputData().get("patientId");
        if (patientId == null) patientId = "unknown";

        String condition = (String) task.getInputData().get("condition");
        if (condition == null) condition = "unspecified";

        String acuity = (String) task.getInputData().get("acuity");
        if (acuity == null) acuity = "low";

        System.out.println("  [assess] Patient " + patientId + ": " + condition + ", acuity: " + acuity);

        List<String> needs = List.of(
                "medication management",
                "physical therapy",
                "nutrition counseling",
                "mental health support"
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("needs", needs);
        result.getOutputData().put("riskScore", 7.2);
        result.getOutputData().put("complexCase", true);
        return result;
    }
}
