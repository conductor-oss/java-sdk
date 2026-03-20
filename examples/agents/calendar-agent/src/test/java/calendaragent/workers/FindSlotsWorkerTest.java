package calendaragent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FindSlotsWorkerTest {

    private final FindSlotsWorker worker = new FindSlotsWorker();

    @Test
    void taskDefName() {
        assertEquals("cl_find_slots", worker.getTaskDefName());
    }

    @Test
    void returnsBestSlot() {
        Task task = taskWith(Map.of(
                "availability", Map.of(
                        "2026-03-12", List.of(Map.of("start", "09:00", "end", "10:00",
                                "available", List.of("alice@company.com", "bob@company.com", "carol@company.com")))),
                "durationMinutes", 60,
                "preferences", Map.of("preferMorning", true, "avoidFriday", true, "timezone", "America/New_York")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> bestSlot = (Map<String, Object>) result.getOutputData().get("bestSlot");
        assertNotNull(bestSlot);
        assertEquals("2026-03-12", bestSlot.get("date"));
        assertEquals("09:00", bestSlot.get("start"));
        assertEquals("10:00", bestSlot.get("end"));
    }

    @Test
    void bestSlotHasHighScore() {
        Task task = taskWith(Map.of(
                "availability", Map.of(),
                "durationMinutes", 60,
                "preferences", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestSlot = (Map<String, Object>) result.getOutputData().get("bestSlot");
        assertEquals(0.95, bestSlot.get("score"));
        assertEquals(true, bestSlot.get("allAttendees"));
    }

    @Test
    void returnsAlternatives() {
        Task task = taskWith(Map.of(
                "availability", Map.of(),
                "durationMinutes", 60,
                "preferences", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alternatives =
                (List<Map<String, Object>>) result.getOutputData().get("alternatives");
        assertNotNull(alternatives);
        assertEquals(2, alternatives.size());
    }

    @Test
    void alternativesHaveLowerScores() {
        Task task = taskWith(Map.of(
                "availability", Map.of(),
                "durationMinutes", 60,
                "preferences", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestSlot = (Map<String, Object>) result.getOutputData().get("bestSlot");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> alternatives =
                (List<Map<String, Object>>) result.getOutputData().get("alternatives");

        double bestScore = (double) bestSlot.get("score");
        for (Map<String, Object> alt : alternatives) {
            assertTrue((double) alt.get("score") < bestScore);
        }
    }

    @Test
    void returnsTotalCandidates() {
        Task task = taskWith(Map.of(
                "availability", Map.of(),
                "durationMinutes", 60,
                "preferences", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("totalCandidates"));
    }

    @Test
    void bestSlotIncludesReason() {
        Task task = taskWith(Map.of(
                "availability", Map.of(),
                "durationMinutes", 60,
                "preferences", Map.of()));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> bestSlot = (Map<String, Object>) result.getOutputData().get("bestSlot");
        assertNotNull(bestSlot.get("reason"));
        assertTrue(((String) bestSlot.get("reason")).contains("Morning"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("bestSlot"));
    }

    @Test
    void handlesNullAvailability() {
        Map<String, Object> input = new HashMap<>();
        input.put("availability", null);
        input.put("durationMinutes", null);
        input.put("preferences", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("bestSlot"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
