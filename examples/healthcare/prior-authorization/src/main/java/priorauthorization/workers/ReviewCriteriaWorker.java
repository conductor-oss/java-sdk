package priorauthorization.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class ReviewCriteriaWorker implements Worker {

    @Override
    public String getTaskDefName() { return "pa_review_criteria"; }

    @Override
    public TaskResult execute(Task task) {
        String reason = String.valueOf(task.getInputData().getOrDefault("clinicalReason", ""));
        String decision;
        int validDays = 0;
        String denyReason = "";
        String reviewNotes = "";

        if (reason.contains("medically necessary")) {
            decision = "approve";
            validDays = 90;
        } else if (reason.contains("cosmetic")) {
            decision = "deny";
            denyReason = "Not medically necessary — cosmetic procedure";
        } else {
            decision = "review";
            reviewNotes = "Additional clinical documentation needed";
        }
        System.out.println("  [review] Procedure: " + task.getInputData().get("procedure")
                + ", Decision: " + decision.toUpperCase());

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("decision", decision);
        output.put("validDays", validDays);
        output.put("denyReason", denyReason);
        output.put("reviewNotes", reviewNotes);
        result.setOutputData(output);
        return result;
    }
}
