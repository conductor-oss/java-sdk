package clickstreamanalytics.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SessionizeWorkerTest {

    private final SessionizeWorker worker = new SessionizeWorker();

    @Test
    void taskDefName() {
        assertEquals("ck_sessionize", worker.getTaskDefName());
    }

    @SuppressWarnings("unchecked")
    @Test
    void groupsEventsByUser() {
        List<Map<String, Object>> events = List.of(
                Map.of("userId", "U1", "page", "/home", "ts", 1000, "action", "view"),
                Map.of("userId", "U1", "page", "/products", "ts", 1030, "action", "view"),
                Map.of("userId", "U2", "page", "/home", "ts", 1010, "action", "view"));
        Task task = taskWith(Map.of("events", events, "sessionTimeout", 1800));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(2, result.getOutputData().get("sessionCount"));

        List<Map<String, Object>> sessions = (List<Map<String, Object>>) result.getOutputData().get("sessions");
        assertEquals(2, sessions.size());
        assertEquals("U1", sessions.get(0).get("userId"));
        assertEquals(2, sessions.get(0).get("clicks"));
    }

    @Test
    void calculatesAvgDuration() {
        List<Map<String, Object>> events = List.of(
                Map.of("userId", "U1", "page", "/home", "ts", 1000, "action", "view"),
                Map.of("userId", "U1", "page", "/products", "ts", 1100, "action", "view"));
        Task task = taskWith(Map.of("events", events));
        TaskResult result = worker.execute(task);

        assertEquals("100s", result.getOutputData().get("avgDuration"));
    }

    @Test
    void handlesNullEvents() {
        Map<String, Object> input = new HashMap<>();
        input.put("events", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("sessionCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
