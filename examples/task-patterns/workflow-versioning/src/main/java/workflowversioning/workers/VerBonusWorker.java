package workflowversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Bonus worker for the versioned workflow.
 * Takes a base result and adds 10.
 */
public class VerBonusWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ver_bonus";
    }

    @Override
    public TaskResult execute(Task task) {
        Object rawBaseResult = task.getInputData().get("baseResult");
        int baseResult = (rawBaseResult instanceof Number) ? ((Number) rawBaseResult).intValue() : 0;

        int bonusResult = baseResult + 10;
        System.out.println("  [ver_bonus] Adding bonus: " + baseResult + " + 10 = " + bonusResult);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("result", bonusResult);
        return result;
    }
}
