package eventbatching.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates batches of events from the collected events list.
 *
 * Input:  {events: [...], batchSize: N}
 * Output: {batches: [[batch1], [batch2], ...], batchCount: number}
 *
 * Uses FIXED output: always produces 2 batches of 3 items each for the
 * standard 6-event input, yielding batchCount=2.
 */
public class CreateBatchesWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "eb_create_batches";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");
        int batchSize = 3;
        Object batchSizeObj = task.getInputData().get("batchSize");
        if (batchSizeObj instanceof Number) {
            batchSize = ((Number) batchSizeObj).intValue();
        }

        if (events == null) {
            events = List.of();
        }

        List<List<Map<String, Object>>> batches = new ArrayList<>();
        for (int i = 0; i < events.size(); i += batchSize) {
            int end = Math.min(i + batchSize, events.size());
            batches.add(new ArrayList<>(events.subList(i, end)));
        }

        int batchCount = batches.size();

        System.out.println("  [eb_create_batches] Created " + batchCount
                + " batches of size " + batchSize);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("batches", batches);
        result.getOutputData().put("batchCount", batchCount);
        return result;
    }
}
