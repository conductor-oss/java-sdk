package ragknowledgegraph.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GraphTraverseWorkerTest {

    private final GraphTraverseWorker worker = new GraphTraverseWorker();

    @Test
    void taskDefName() {
        assertEquals("kg_graph_traverse", worker.getTaskDefName());
    }

    @Test
    void returnsSevenFacts() {
        List<Map<String, String>> entities = List.of(
                Map.of("name", "Conductor", "type", "Software", "id", "entity-1")
        );
        Task task = taskWith(new HashMap<>(Map.of(
                "entities", entities,
                "maxDepth", 2
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> facts =
                (List<Map<String, Object>>) result.getOutputData().get("facts");
        assertNotNull(facts);
        assertEquals(7, facts.size());
    }

    @Test
    void factsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("maxDepth", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> facts =
                (List<Map<String, Object>>) result.getOutputData().get("facts");

        for (Map<String, Object> fact : facts) {
            assertNotNull(fact.get("subject"));
            assertNotNull(fact.get("predicate"));
            assertNotNull(fact.get("object"));
            assertNotNull(fact.get("confidence"));
            assertInstanceOf(String.class, fact.get("subject"));
            assertInstanceOf(String.class, fact.get("predicate"));
            assertInstanceOf(String.class, fact.get("object"));
            assertInstanceOf(Double.class, fact.get("confidence"));
        }
    }

    @Test
    void returnsFourRelations() {
        Task task = taskWith(new HashMap<>(Map.of("maxDepth", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relations =
                (List<Map<String, Object>>) result.getOutputData().get("relations");
        assertNotNull(relations);
        assertEquals(4, relations.size());
    }

    @Test
    void relationsHaveRequiredFields() {
        Task task = taskWith(new HashMap<>(Map.of("maxDepth", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> relations =
                (List<Map<String, Object>>) result.getOutputData().get("relations");

        for (Map<String, Object> rel : relations) {
            assertNotNull(rel.get("from"));
            assertNotNull(rel.get("to"));
            assertNotNull(rel.get("type"));
            assertNotNull(rel.get("depth"));
        }
    }

    @Test
    void factsContainExpectedContent() {
        Task task = taskWith(new HashMap<>(Map.of("maxDepth", 2)));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> facts =
                (List<Map<String, Object>>) result.getOutputData().get("facts");

        assertEquals("Conductor", facts.get(0).get("subject"));
        assertEquals("developed_by", facts.get(0).get("predicate"));
        assertEquals("Netflix", facts.get(0).get("object"));
        assertEquals(0.99, facts.get(0).get("confidence"));
    }

    @Test
    void handlesNullEntities() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("facts"));
        assertNotNull(result.getOutputData().get("relations"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
