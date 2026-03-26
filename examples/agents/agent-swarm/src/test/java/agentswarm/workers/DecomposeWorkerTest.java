package agentswarm.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DecomposeWorkerTest {

    private final DecomposeWorker worker = new DecomposeWorker();

    @Test
    void taskDefName() {
        assertEquals("as_decompose", worker.getTaskDefName());
    }

    @Test
    void decomposesTopicIntoFourSubtasks() {
        Task task = taskWith(Map.of("researchTopic", "Impact of AI on healthcare"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("subtaskCount"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) result.getOutputData().get("subtasks");
        assertNotNull(subtasks);
        assertEquals(4, subtasks.size());
    }

    @Test
    void subtasksHaveRequiredFields() {
        Task task = taskWith(Map.of("researchTopic", "Quantum computing"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) result.getOutputData().get("subtasks");
        for (Map<String, Object> subtask : subtasks) {
            assertTrue(subtask.containsKey("id"), "Subtask must have id");
            assertTrue(subtask.containsKey("area"), "Subtask must have area");
            assertTrue(subtask.containsKey("instruction"), "Subtask must have instruction");
        }
    }

    @Test
    void subtasksHaveDistinctAreas() {
        Task task = taskWith(Map.of("researchTopic", "Cloud computing"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) result.getOutputData().get("subtasks");
        List<String> areas = subtasks.stream()
                .map(s -> (String) s.get("area"))
                .toList();
        assertEquals(4, areas.stream().distinct().count(), "All areas must be distinct");
        assertTrue(areas.contains("Market Analysis"));
        assertTrue(areas.contains("Technical Landscape"));
        assertTrue(areas.contains("Use Cases"));
        assertTrue(areas.contains("Future Trends"));
    }

    @Test
    void subtaskInstructionsContainTopic() {
        String topic = "Blockchain in supply chain";
        Task task = taskWith(Map.of("researchTopic", topic));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) result.getOutputData().get("subtasks");
        for (Map<String, Object> subtask : subtasks) {
            String instruction = (String) subtask.get("instruction");
            assertTrue(instruction.contains(topic),
                    "Instruction should reference the research topic");
        }
    }

    @Test
    void subtasksHaveDistinctIds() {
        Task task = taskWith(Map.of("researchTopic", "Robotics"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtasks = (List<Map<String, Object>>) result.getOutputData().get("subtasks");
        List<String> ids = subtasks.stream()
                .map(s -> (String) s.get("id"))
                .toList();
        assertEquals(4, ids.stream().distinct().count(), "All IDs must be distinct");
    }

    @Test
    void handlesNullTopic() {
        Map<String, Object> input = new HashMap<>();
        input.put("researchTopic", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("subtaskCount"));
    }

    @Test
    void handlesMissingTopic() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subtasks"));
    }

    @Test
    void handlesBlankTopic() {
        Task task = taskWith(Map.of("researchTopic", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(4, result.getOutputData().get("subtaskCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
