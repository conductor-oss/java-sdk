package emailapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Worker for ea_process_decision — processes the approval decision.
 *
 * Reads the decision from the input (provided after the WAIT task completes)
 * and returns { processed: true }.
 */
public class ProcessDecisionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ea_process_decision";
    }

    @Override
    public TaskResult execute(Task task) {
        Object decision = task.getInputData().get("decision");
        System.out.println("  [ea_process_decision] Processing decision: " + decision);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processed", true);
        return result;
    }
}
