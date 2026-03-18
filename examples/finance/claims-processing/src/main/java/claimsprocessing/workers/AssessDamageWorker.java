package claimsprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assesses damage for a claim. Real damage assessment:
 * - Categorizes damage based on amount thresholds
 * - Applies claim-type-specific assessment factors
 * - Computes assessed amount (percentage of requested based on category)
 */
public class AssessDamageWorker implements Worker {

    @Override
    public String getTaskDefName() { return "clp_assess_damage"; }

    @Override
    public TaskResult execute(Task task) {
        Object rawAmount = task.getInputData().get("requestedAmount");
        String claimType = (String) task.getInputData().get("claimType");
        if (claimType == null) claimType = "unknown";

        double requested = 0;
        if (rawAmount instanceof Number) {
            requested = ((Number) rawAmount).doubleValue();
        } else if (rawAmount instanceof String) {
            try { requested = Double.parseDouble((String) rawAmount); } catch (NumberFormatException ignored) {}
        }

        // Real damage categorization
        String damageCategory;
        double assessmentFactor;
        if (requested > 50000) {
            damageCategory = "severe";
            assessmentFactor = 0.75; // severe claims get 75% initial assessment
        } else if (requested > 10000) {
            damageCategory = "major";
            assessmentFactor = 0.80;
        } else if (requested > 2000) {
            damageCategory = "moderate";
            assessmentFactor = 0.85;
        } else {
            damageCategory = "minor";
            assessmentFactor = 0.95; // minor claims assessed closer to requested
        }

        long assessed = Math.round(requested * assessmentFactor);

        String assessorNotes = "Damage category: " + damageCategory
                + ". Assessment factor: " + (int)(assessmentFactor * 100) + "% applied to $" + (long) requested
                + " claim (" + claimType + ").";

        System.out.println("  [assess] Requested: $" + (long) requested + ", Assessed: $" + assessed
                + " (" + damageCategory + ", factor: " + assessmentFactor + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assessedAmount", assessed);
        result.getOutputData().put("damageCategory", damageCategory);
        result.getOutputData().put("assessmentFactor", assessmentFactor);
        result.getOutputData().put("assessorNotes", assessorNotes);
        return result;
    }
}
