package switchplusfork.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Lane B batch processor.
 * Processes items in parallel lane B and returns the lane identifier and item count.
 */
public class ProcessBWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sf_process_b";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Object> items = (List<Object>) task.getInputData().get("items");
        int count = (items != null) ? items.size() : 0;

        System.out.println("  [sf_process_b] Lane B processing " + count + " items");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("lane", "B");
        result.getOutputData().put("count", count);
        return result;
    }
}
