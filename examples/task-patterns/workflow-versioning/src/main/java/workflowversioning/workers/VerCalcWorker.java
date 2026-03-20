package workflowversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Calculation worker for the versioned workflow.
 * Takes a numeric value and returns value * 2.
 */
public class VerCalcWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ver_calc";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawValue = task.getInputData().get("value");
        int value = (rawValue instanceof Number) ? ((Number) rawValue).intValue() : 0;

        System.out.println("  [ver_calc] Calculating: " + value + " * 2 = " + (value * 2));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", value * 2);
        return result;
    }
}
