package eventordering.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Processes the next event from the sorted list based on the current
 * DO_WHILE iteration index. Gets the event at position (iteration)
 * and outputs its sequence number along with the iteration.
 */
public class ProcessNextWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "oo_process_next";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> sortedEvents =
                (List<Map<String, Object>>) task.getInputData().get("sortedEvents");
        Object iterObj = task.getInputData().get("iteration");
        int iteration = (iterObj instanceof Number) ? ((Number) iterObj).intValue() : 0;

        System.out.println("  [oo_process_next] Processing event at index " + iteration);

        int processedSeq = 0;
        if (sortedEvents != null && iteration < sortedEvents.size()) {
            Map<String, Object> event = sortedEvents.get(iteration);
            Object seqObj = event.get("seq");
            processedSeq = (seqObj instanceof Number) ? ((Number) seqObj).intValue() : 0;
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("processedSeq", processedSeq);
        result.getOutputData().put("iteration", iteration);
        return result;
    }
}
