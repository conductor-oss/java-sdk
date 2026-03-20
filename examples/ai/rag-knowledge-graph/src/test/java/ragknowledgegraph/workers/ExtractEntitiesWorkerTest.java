package ragknowledgegraph.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ExtractEntitiesWorkerTest {

    private final ExtractEntitiesWorker worker = new ExtractEntitiesWorker();

    @Test
    void taskDefName() {
        assertEquals("kg_extract_entities", worker.getTaskDefName());
    }

    @Test
    void returnsFourEntities() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "What is Conductor?"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, String>> entities =
                (List<Map<String, String>>) result.getOutputData().get("entities");
        assertNotNull(entities);
        assertEquals(4, entities.size());
    }

    @Test
    void entitiesHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> entities =
                (List<Map<String, String>>) result.getOutputData().get("entities");

        for (Map<String, String> entity : entities) {
            assertNotNull(entity.get("name"));
            assertNotNull(entity.get("type"));
            assertNotNull(entity.get("id"));
        }
    }

    @Test
    void entitiesHaveExpectedValues() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> entities =
                (List<Map<String, String>>) result.getOutputData().get("entities");

        assertEquals("Conductor", entities.get(0).get("name"));
        assertEquals("Software", entities.get(0).get("type"));
        assertEquals("Netflix", entities.get(1).get("name"));
        assertEquals("Organization", entities.get(1).get("type"));
        assertEquals("microservices", entities.get(2).get("name"));
        assertEquals("Architecture", entities.get(2).get("type"));
        assertEquals("workflow orchestration", entities.get(3).get("name"));
        assertEquals("Concept", entities.get(3).get("type"));
    }

    @Test
    void entitiesHaveUniqueIds() {
        Task task = taskWith(new HashMap<>(Map.of("question", "test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, String>> entities =
                (List<Map<String, String>>) result.getOutputData().get("entities");

        long uniqueIds = entities.stream()
                .map(e -> e.get("id"))
                .distinct()
                .count();
        assertEquals(4, uniqueIds);
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("entities"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
