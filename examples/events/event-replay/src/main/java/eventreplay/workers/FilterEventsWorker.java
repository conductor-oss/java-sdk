package eventreplay.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Filters events based on the provided filter criteria (status and eventType).
 * With status="failed" and eventType="order.created", returns 2 matching events.
 */
public class FilterEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ep_filter_events";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> events =
                (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        Map<String, Object> filterCriteria =
                (Map<String, Object>) task.getInputData().get("filterCriteria");

        String statusFilter = null;
        String eventTypeFilter = null;
        if (filterCriteria != null) {
            statusFilter = (String) filterCriteria.get("status");
            eventTypeFilter = (String) filterCriteria.get("eventType");
        }

        System.out.println("  [ep_filter_events] Filtering " + events.size()
                + " events by status=" + statusFilter + ", eventType=" + eventTypeFilter);

        List<Map<String, Object>> filteredEvents = new ArrayList<>();
        for (Map<String, Object> event : events) {
            boolean matches = true;
            if (statusFilter != null && !statusFilter.equals(event.get("status"))) {
                matches = false;
            }
            if (eventTypeFilter != null && !eventTypeFilter.equals(event.get("type"))) {
                matches = false;
            }
            if (matches) {
                filteredEvents.add(event);
            }
        }

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("filteredEvents", filteredEvents);
        result.getOutputData().put("filteredCount", filteredEvents.size());
        return result;
    }
}
