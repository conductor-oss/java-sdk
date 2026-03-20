package creditscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies an applicant based on credit score.
 * Input: applicantId, score
 * Output: classification, approvalLikelihood
 *
 * Classification:
 *   >= 800 -> Exceptional
 *   >= 740 -> Very Good
 *   >= 670 -> Good
 *   >= 580 -> Fair
 *   < 580  -> Poor
 */
public class ClassifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "csc_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        int score = toInt(task.getInputData().get("score"));

        String classification;
        if (score >= 800) {
            classification = "Exceptional";
        } else if (score >= 740) {
            classification = "Very Good";
        } else if (score >= 670) {
            classification = "Good";
        } else if (score >= 580) {
            classification = "Fair";
        } else {
            classification = "Poor";
        }

        String approvalLikelihood = score >= 670 ? "high" : "low";

        System.out.println("  [classify] Score " + score + ": " + classification);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classification", classification);
        result.getOutputData().put("approvalLikelihood", approvalLikelihood);
        return result;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try { return Integer.parseInt(value.toString()); } catch (NumberFormatException e) { return 0; }
    }
}
