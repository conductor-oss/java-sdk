package loanorigination.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Underwrites the loan based on credit score and DTI ratio.
 * Input: applicationId, creditScore, loanAmount, dti
 * Output: decision, interestRate, term, conditions
 *
 * Decision logic:
 *   score >= 680 AND dti <= 43 -> "approved", else "declined"
 * Rate logic:
 *   score >= 760 -> 5.25%, score >= 700 -> 5.75%, else 6.5%
 */
public class UnderwriteWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "lnr_underwrite";
    }

    @Override
    public TaskResult execute(Task task) {
        int score = toInt(task.getInputData().get("creditScore"));
        int dti = toInt(task.getInputData().get("dti"));

        String decision = (score >= 680 && dti <= 43) ? "approved" : "declined";
        double rate;
        if (score >= 760) {
            rate = 5.25;
        } else if (score >= 700) {
            rate = 5.75;
        } else {
            rate = 6.5;
        }

        System.out.println("  [underwrite] Score: " + score + ", DTI: " + dti + "%, Decision: " + decision + ", Rate: " + rate + "%");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", decision);
        result.getOutputData().put("interestRate", rate);
        result.getOutputData().put("term", 360);
        result.getOutputData().put("conditions", List.of());
        return result;
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
