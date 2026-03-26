package customerchurn.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectRiskWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccn_detect_risk";
    }

    @Override
    public TaskResult execute(Task task) {

        String customerId = (String) task.getInputData().get("customerId");
        System.out.printf("  [detect] Customer %s churn risk score: 0.82%n", customerId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("riskScore", 0.82);
        result.getOutputData().put("riskLevel", "high");
        return result;
    }
}
