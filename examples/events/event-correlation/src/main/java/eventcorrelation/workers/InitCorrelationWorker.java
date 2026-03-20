package eventcorrelation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Initializes a correlation session with a fixed correlation ID and timestamp.
 * Returns deterministic output with no randomness.
 */
public class InitCorrelationWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ec_init_correlation";
    }

    @Override
    public TaskResult execute(Task task) {
        String correlationId = (String) task.getInputData().get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "corr-unknown";
        }

        Object expectedEventsObj = task.getInputData().get("expectedEvents");
        int expectedEvents = 3;
        if (expectedEventsObj instanceof Number) {
            expectedEvents = ((Number) expectedEventsObj).intValue();
        }

        System.out.println("  [ec_init_correlation] Initializing correlation session: " + correlationId
                + " (expecting " + expectedEvents + " events)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("correlationId", correlationId);
        result.getOutputData().put("sessionStarted", "2026-01-15T10:00:00Z");
        return result;
    }
}
