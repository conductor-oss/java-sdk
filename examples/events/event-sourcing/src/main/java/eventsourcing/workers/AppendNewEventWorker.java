package eventsourcing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Appends a new domain event to the existing event list. Assigns the next
 * sequence number and a fixed timestamp, then returns the combined list.
 */
public class AppendNewEventWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ev_append_new_event";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        String aggregateId = (String) task.getInputData().get("aggregateId");
        List<Map<String, Object>> existingEvents =
                (List<Map<String, Object>>) task.getInputData().get("existingEvents");
        Map<String, Object> newEvent =
                (Map<String, Object>) task.getInputData().get("newEvent");
        int nextSequence = task.getInputData().get("nextSequence") instanceof Number
                ? ((Number) task.getInputData().get("nextSequence")).intValue()
                : 5;

        if (existingEvents == null) {
            existingEvents = List.of();
        }
        if (newEvent == null) {
            newEvent = Map.of();
        }

        System.out.println("  [ev_append_new_event] Appending event seq " + nextSequence
                + " to aggregate " + aggregateId);

        // Build the full event with sequence number and fixed timestamp
        Map<String, Object> fullEvent = new LinkedHashMap<>();
        fullEvent.put("sequence", nextSequence);
        fullEvent.put("type", newEvent.getOrDefault("type", "Unknown"));
        fullEvent.put("timestamp", "2026-03-08T10:05:00Z");
        fullEvent.put("data", newEvent.getOrDefault("data", Map.of()));

        // Combine existing events with the new one
        List<Map<String, Object>> allEvents = new ArrayList<>(existingEvents);
        allEvents.add(fullEvent);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("allEvents", allEvents);
        result.getOutputData().put("totalEvents", allEvents.size());
        result.getOutputData().put("appendedEvent", fullEvent);
        return result;
    }
}
