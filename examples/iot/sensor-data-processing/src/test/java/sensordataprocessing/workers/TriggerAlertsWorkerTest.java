package sensordataprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TriggerAlertsWorkerTest {

    private final TriggerAlertsWorker worker = new TriggerAlertsWorker();

    @Test
    void taskDefName() {
        assertEquals("sen_trigger_alerts", worker.getTaskDefName());
    }

    @Test
    void triggersAlertWhenAnomaliesExist() {
        Task task = taskWith(Map.of(
                "anomalies", List.of(Map.of("type", "high_temperature")),
                "trend", "rising"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(1, result.getOutputData().get("alertsTriggered"));
    }

    @Test
    void noAlertWhenNoAnomalies() {
        Task task = taskWith(Map.of("anomalies", List.of(), "trend", "stable"));
        TaskResult result = worker.execute(task);

        assertEquals(0, result.getOutputData().get("alertsTriggered"));
    }

    @Test
    void escalationLevelIsWarningWhenAlerts() {
        Task task = taskWith(Map.of(
                "anomalies", List.of(Map.of("type", "high_temp")),
                "trend", "rising"));
        TaskResult result = worker.execute(task);

        assertEquals("warning", result.getOutputData().get("escalationLevel"));
    }

    @Test
    void escalationLevelIsNoneWhenNoAlerts() {
        Task task = taskWith(Map.of("anomalies", List.of(), "trend", "stable"));
        TaskResult result = worker.execute(task);

        assertEquals("none", result.getOutputData().get("escalationLevel"));
    }

    @Test
    void notificationsPopulatedWhenAlerts() {
        Task task = taskWith(Map.of(
                "anomalies", List.of(Map.of("type", "high_temp")),
                "trend", "rising"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> notifications = (List<String>) result.getOutputData().get("notifications");
        assertEquals(2, notifications.size());
    }

    @Test
    void notificationsEmptyWhenNoAlerts() {
        Task task = taskWith(Map.of("anomalies", List.of(), "trend", "stable"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<?> notifications = (List<?>) result.getOutputData().get("notifications");
        assertTrue(notifications.isEmpty());
    }

    @Test
    void handlesNullAnomalies() {
        Map<String, Object> input = new HashMap<>();
        input.put("anomalies", null);
        input.put("trend", "stable");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("alertsTriggered"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("escalationLevel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
