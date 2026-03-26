package trainingdatalabeling.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Worker for tdl_compute_agreement task -- compares two annotators' label arrays
 * and computes inter-annotator agreement.
 *
 * Inputs:
 *   - labels1: List of label strings from annotator 1
 *   - labels2: List of label strings from annotator 2
 *
 * Outputs:
 *   - agreements:    count of matching labels
 *   - disagreements: count of differing labels
 *   - total:         total number of items compared
 *   - agreementPct:  agreement percentage (0.0 - 100.0)
 */
public class ComputeAgreementWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "tdl_compute_agreement";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        System.out.println("  [tdl_compute_agreement] Computing inter-annotator agreement...");

        List<String> labels1 = (List<String>) task.getInputData().get("labels1");
        List<String> labels2 = (List<String>) task.getInputData().get("labels2");

        int total = Math.min(labels1.size(), labels2.size());
        int agreements = 0;
        int disagreements = 0;

        for (int i = 0; i < total; i++) {
            if (labels1.get(i).equals(labels2.get(i))) {
                agreements++;
            } else {
                disagreements++;
            }
        }

        double agreementPct = total > 0 ? (agreements * 100.0) / total : 0.0;

        System.out.println("  [tdl_compute_agreement] Agreement: " + agreements + "/" + total
                + " (" + agreementPct + "%)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("agreements", agreements);
        result.getOutputData().put("disagreements", disagreements);
        result.getOutputData().put("total", total);
        result.getOutputData().put("agreementPct", agreementPct);

        return result;
    }
}
