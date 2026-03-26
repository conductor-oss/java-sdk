package changemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Assesses risk level for a submitted change request.
 * Input: assess_riskData (from submit output)
 * Output: riskLevel, riskScore, assessed, requiresApproval
 */
public class AssessRisk implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_assess_risk";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("assess_riskData");
        String changeType = "standard";
        if (data != null && data.get("changeType") != null) {
            changeType = (String) data.get("changeType");
        }

        String riskLevel;
        int riskScore;
        boolean requiresApproval;

        switch (changeType) {
            case "emergency":
                riskLevel = "high";
                riskScore = 85;
                requiresApproval = true;
                break;
            case "normal":
                riskLevel = "medium";
                riskScore = 50;
                requiresApproval = true;
                break;
            default:
                riskLevel = "low";
                riskScore = 25;
                requiresApproval = false;
                break;
        }

        System.out.println("[cm_assess_risk] Risk assessment: " + riskLevel + " (score: " + riskScore + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("riskLevel", riskLevel);
        output.put("riskScore", riskScore);
        output.put("assessed", true);
        output.put("requiresApproval", requiresApproval);
        result.setOutputData(output);
        return result;
    }
}
