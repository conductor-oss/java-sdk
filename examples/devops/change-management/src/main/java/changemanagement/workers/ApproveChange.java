package changemanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Processes CAB (Change Advisory Board) approval for a change.
 * Input: approveData (from assess_risk output)
 * Output: approved, approver, approvalNote
 */
public class ApproveChange implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_approve";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("approveData");
        String riskLevel = "low";
        if (data != null && data.get("riskLevel") != null) {
            riskLevel = (String) data.get("riskLevel");
        }

        boolean approved;
        String approvalNote;

        if ("high".equals(riskLevel)) {
            approved = true;
            approvalNote = "Approved with additional monitoring required";
        } else if ("medium".equals(riskLevel)) {
            approved = true;
            approvalNote = "Approved by CAB with standard review";
        } else {
            approved = true;
            approvalNote = "Auto-approved for low-risk change";
        }

        System.out.println("[cm_approve] CAB decision: " + (approved ? "approved" : "rejected") + " — " + approvalNote);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("approved", approved);
        output.put("approver", "cab-board");
        output.put("approvalNote", approvalNote);
        result.setOutputData(output);
        return result;
    }
}
