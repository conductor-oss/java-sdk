package eventreplaytesting.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Loads recorded events for replay testing.
 * Input: testSuiteId, eventSource
 * Output: events (list of recorded events), count
 */
public class LoadEventsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "rt_load_events";
    }

    @Override
    public TaskResult execute(Task task) {
        String testSuiteId = (String) task.getInputData().get("testSuiteId");
        if (testSuiteId == null) {
            testSuiteId = "unknown";
        }

        String eventSource = (String) task.getInputData().get("eventSource");
        if (eventSource == null) {
            eventSource = "default";
        }

        System.out.println("  [rt_load_events] Loading events for suite: " + testSuiteId + " from " + eventSource);

        List<Map<String, String>> events = List.of(
                Map.of("id", "rec-1", "type", "order.created", "expected", "processed"),
                Map.of("id", "rec-2", "type", "payment.received", "expected", "processed"),
                Map.of("id", "rec-3", "type", "shipping.label", "expected", "processed")
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("events", events);
        result.getOutputData().put("count", events.size());
        return result;
    }
}
