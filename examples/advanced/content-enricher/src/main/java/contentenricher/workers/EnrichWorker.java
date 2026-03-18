package contentenricher.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Enriches the original message with lookup data.
 * Merges all fields from the original message and the lookup data,
 * then adds an ISO-8601 enrichedAt timestamp using the real system clock.
 *
 * Input: originalMessage, lookupData
 * Output: enrichedMessage
 */
public class EnrichWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "enr_enrich";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        Object origObj = task.getInputData().get("originalMessage");
        Map<String, Object> original = (origObj instanceof Map) ? (Map<String, Object>) origObj : Map.of();

        Object lookupObj = task.getInputData().get("lookupData");
        Map<String, Object> lookup = (lookupObj instanceof Map) ? (Map<String, Object>) lookupObj : Map.of();

        Map<String, Object> enriched = new HashMap<>(original);
        enriched.putAll(lookup);
        enriched.put("enrichedAt", Instant.now().toString());

        System.out.println("  [enrich] Message enriched with " + lookup.size() + " additional fields");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enrichedMessage", enriched);
        return result;
    }
}
