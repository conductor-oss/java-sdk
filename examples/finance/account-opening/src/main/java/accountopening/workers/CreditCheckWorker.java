package accountopening.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Performs a real algorithmic credit/ChexSystems check for account opening.
 * Computes a ChexSystems score based on name hash for deterministic results.
 */
public class CreditCheckWorker implements Worker {

    @Override
    public String getTaskDefName() { return "acc_credit_check"; }

    @Override
    public TaskResult execute(Task task) {
        String applicantName = (String) task.getInputData().get("applicantName");
        if (applicantName == null) applicantName = "UNKNOWN";

        // Deterministic score based on name hash (0-999 range for ChexSystems)
        int nameHash = Math.abs(applicantName.hashCode());
        int chexScore = nameHash % 1000;
        int bankruptcies = (nameHash / 1000) % 3;    // 0-2
        int closedForCause = (nameHash / 3000) % 2;  // 0-1

        // Eligibility: score < 700 and no bankruptcies or closedForCause
        boolean eligible = chexScore < 700 && bankruptcies == 0 && closedForCause == 0;

        System.out.println("  [credit] ChexSystems check for " + applicantName
                + " - score: " + chexScore + ", bankruptcies: " + bankruptcies
                + ", closedForCause: " + closedForCause + ", eligible: " + eligible);

        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("chexScore", chexScore);
        r.getOutputData().put("bankruptcies", bankruptcies);
        r.getOutputData().put("closedForCause", closedForCause);
        r.getOutputData().put("eligible", eligible);
        return r;
    }
}
