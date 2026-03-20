package teamsintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReceiveWebhookWorkerTest {

    private final ReceiveWebhookWorker worker = new ReceiveWebhookWorker();

    @Test
    void taskDefName() {
        assertEquals("tms_receive_webhook", worker.getTaskDefName());
    }

    @Test
    void receivesWebhookForTeam() {
        Task task = taskWith(Map.of("teamId", "team-eng-01", "channelId", "ch-alerts",
                "payload", Map.of("severity", "high")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("alert", result.getOutputData().get("eventType"));
        assertNotNull(result.getOutputData().get("data"));
        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void passesPayloadAsData() {
        Map<String, Object> payload = Map.of("source", "monitoring", "message", "CPU high");
        Task task = taskWith(Map.of("teamId", "t1", "channelId", "c1", "payload", payload));
        TaskResult result = worker.execute(task);

        assertEquals(payload, result.getOutputData().get("data"));
    }

    @Test
    void handlesNullTeamId() {
        Map<String, Object> input = new HashMap<>();
        input.put("teamId", null);
        input.put("channelId", "c1");
        input.put("payload", Map.of());
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("alert", result.getOutputData().get("eventType"));
    }

    @Test
    void alwaysReturnsAlertEventType() {
        Task task = taskWith(Map.of("teamId", "any", "channelId", "any", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals("alert", result.getOutputData().get("eventType"));
    }

    @Test
    void outputContainsReceivedAt() {
        Task task = taskWith(Map.of("teamId", "t1", "channelId", "c1", "payload", Map.of()));
        TaskResult result = worker.execute(task);

        assertNotNull(result.getOutputData().get("receivedAt"));
    }

    @Test
    void handlesNullPayload() {
        Map<String, Object> input = new HashMap<>();
        input.put("teamId", "t1");
        input.put("channelId", "c1");
        input.put("payload", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
