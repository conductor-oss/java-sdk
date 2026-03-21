package datasync.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Applies resolved updates to both systems.
 * Input: toApplyA (list), toApplyB (list), systemA, systemB
 * Output: appliedToA, appliedToB, totalApplied
 */
public class ApplyUpdatesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sy_apply_updates";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> toA =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("toApplyA", List.of());
        List<Map<String, Object>> toB =
                (List<Map<String, Object>>) task.getInputData().getOrDefault("toApplyB", List.of());

        System.out.println("  [apply] Applied " + toA.size() + " updates to system A, "
                + toB.size() + " updates to system B");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("appliedToA", toA.size());
        result.getOutputData().put("appliedToB", toB.size());
        result.getOutputData().put("totalApplied", toA.size() + toB.size());
        return result;
    }
}
