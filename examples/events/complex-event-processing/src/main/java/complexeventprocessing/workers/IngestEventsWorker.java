package complexeventprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Ingests a list of events and passes them through with a count.
 * Input: events (list of event maps)
 * Output: events, count (size of the list)
 */
public class IngestEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cp_ingest_events";
    }

    @Override
    public TaskResult execute(Task task) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> events = (List<Map<String, Object>>) task.getInputData().get("events");
        if (events == null) {
            events = List.of();
        }

        System.out.println("  [cp_ingest_events] Ingested " + events.size() + " events for pattern analysis");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("count", events.size());
        return result;
    }
}
