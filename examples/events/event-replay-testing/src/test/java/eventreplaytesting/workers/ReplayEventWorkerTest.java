package eventreplaytesting.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ReplayEventWorkerTest {

    private final ReplayEventWorker worker = new ReplayEventWorker();

    private static final List<Map<String, Object>> SAMPLE_EVENTS = List.of(
            Map.of("id", "rec-1", "type", "order.created", "expected", "processed"),
            Map.of("id", "rec-2", "type", "payment.received", "expected", "processed"),
            Map.of("id", "rec-3", "type", "shipping.label", "expected", "processed")
    );

    @Test
    void taskDefName() {
        assertEquals("rt_replay_event", worker.getTaskDefName());
    }

    @Test
    void replaysFirstEvent() {
        Task task = taskWith(Map.of(
                "events", SAMPLE_EVENTS,
                "sandboxId", "sandbox_fixed_001",
                "iteration", 0));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("processed", result.getOutputData().get("result"));
        assertEquals("rec-1", result.getOutputData().get("eventId"));
        assertEquals("processed", result.getOutputData().get("expectedResult"));
    }

    @Test
    void replaysSecondEvent() {
        Task task = taskWith(Map.of(
                "events", SAMPLE_EVENTS,
                "sandboxId", "sandbox_fixed_001",
                "iteration", 1));
        TaskResult result = worker.execute(task);

        assertEquals("rec-2", result.getOutputData().get("eventId"));
        assertEquals("processed", result.getOutputData().get("expectedResult"));
    }

    @Test
    void replaysThirdEvent() {
        Task task = taskWith(Map.of(
                "events", SAMPLE_EVENTS,
                "sandboxId", "sandbox_fixed_001",
                "iteration", 2));
        TaskResult result = worker.execute(task);

        assertEquals("rec-3", result.getOutputData().get("eventId"));
        assertEquals("processed", result.getOutputData().get("expectedResult"));
    }

    @Test
    void resultIsAlwaysProcessed() {
        for (int i = 0; i < 3; i++) {
            Task task = taskWith(Map.of(
                    "events", SAMPLE_EVENTS,
                    "sandboxId", "sandbox_fixed_001",
                    "iteration", i));
            TaskResult result = worker.execute(task);
            assertEquals("processed", result.getOutputData().get("result"));
        }
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        input.put("sandboxId", "sandbox_fixed_001");
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
        assertEquals("unknown", result.getOutputData().get("expectedResult"));
    }

    @Test
    void handlesOutOfBoundsIteration() {
        Task task = taskWith(Map.of(
                "events", SAMPLE_EVENTS,
                "sandboxId", "sandbox_fixed_001",
                "iteration", 99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesNullSandboxId() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", SAMPLE_EVENTS);
        input.put("sandboxId", null);
        input.put("iteration", 0);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rec-1", result.getOutputData().get("eventId"));
    }

    @Test
    void handlesMissingIteration() {
        Task task = taskWith(Map.of(
                "events", SAMPLE_EVENTS,
                "sandboxId", "sandbox_fixed_001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("rec-1", result.getOutputData().get("eventId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
