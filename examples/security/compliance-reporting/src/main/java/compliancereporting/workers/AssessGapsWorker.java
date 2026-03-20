package compliancereporting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Assesses gaps in compliance controls.
 * Input: assess_gapsData (from map controls)
 * Output: assess_gaps, processed
 */
public class AssessGapsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_assess_gaps";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [gaps] 2 gaps found: backup verification, access review timeliness");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("assess_gaps", true);
        result.getOutputData().put("processed", true);
        return result;
    }
}
