package authenticationworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Performs risk assessment on the authentication attempt.
 * Input: risk_assessmentData (from previous step)
 * Output: risk_assessment, processed
 */
public class RiskAssessmentWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "auth_risk_assessment";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [risk] Known device, known location — low risk");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("risk_assessment", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
