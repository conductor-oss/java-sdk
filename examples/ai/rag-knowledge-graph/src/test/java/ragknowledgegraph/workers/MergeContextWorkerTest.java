package ragknowledgegraph.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MergeContextWorkerTest {

    private final MergeContextWorker worker = new MergeContextWorker();

    @Test
    void taskDefName() {
        assertEquals("kg_merge_context", worker.getTaskDefName());
    }

    @Test
    void mergesGraphAndVectorContext() {
        List<Map<String, Object>> graphFacts = List.of(
                new HashMap<>(Map.of("subject", "Conductor", "predicate", "developed_by", "object", "Netflix", "confidence", 0.99)),
                new HashMap<>(Map.of("subject", "Conductor", "predicate", "is_a", "object", "workflow engine", "confidence", 0.98))
        );
        List<Map<String, Object>> graphRelations = List.of(
                new HashMap<>(Map.of("from", "entity-1", "to", "entity-2", "type", "developed_by", "depth", 1))
        );
        List<Map<String, Object>> vectorDocs = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Netflix created Conductor", "score", 0.94)),
                new HashMap<>(Map.of("id", "doc-2", "text", "Conductor uses JSON DSL", "score", 0.91))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "graphFacts", graphFacts,
                "graphRelations", graphRelations,
                "vectorDocs", vectorDocs
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        assertNotNull(mergedContext);
        assertNotNull(mergedContext.get("graphSummary"));
        assertNotNull(mergedContext.get("vectorSummary"));
        assertEquals(4, mergedContext.get("totalSources"));
    }

    @Test
    void graphSummaryContainsFactContent() {
        List<Map<String, Object>> graphFacts = List.of(
                new HashMap<>(Map.of("subject", "Conductor", "predicate", "developed_by", "object", "Netflix", "confidence", 0.99))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "graphFacts", graphFacts
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        String graphSummary = (String) mergedContext.get("graphSummary");
        assertTrue(graphSummary.contains("Conductor"));
        assertTrue(graphSummary.contains("developed_by"));
        assertTrue(graphSummary.contains("Netflix"));
    }

    @Test
    void vectorSummaryContainsDocContent() {
        List<Map<String, Object>> vectorDocs = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "Netflix created Conductor", "score", 0.94))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "vectorDocs", vectorDocs
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        String vectorSummary = (String) mergedContext.get("vectorSummary");
        assertTrue(vectorSummary.contains("Netflix created Conductor"));
    }

    @Test
    void totalSourcesCountsFactsAndDocs() {
        List<Map<String, Object>> graphFacts = List.of(
                new HashMap<>(Map.of("subject", "A", "predicate", "b", "object", "C", "confidence", 0.9)),
                new HashMap<>(Map.of("subject", "D", "predicate", "e", "object", "F", "confidence", 0.8)),
                new HashMap<>(Map.of("subject", "G", "predicate", "h", "object", "I", "confidence", 0.7))
        );
        List<Map<String, Object>> vectorDocs = List.of(
                new HashMap<>(Map.of("id", "doc-1", "text", "text1", "score", 0.9))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "graphFacts", graphFacts,
                "vectorDocs", vectorDocs
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        assertEquals(4, mergedContext.get("totalSources"));
    }

    @Test
    void handlesAllNullInputs() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        assertNotNull(mergedContext);
        assertEquals(0, mergedContext.get("totalSources"));
        assertEquals("", mergedContext.get("graphSummary"));
        assertEquals("", mergedContext.get("vectorSummary"));
    }

    @Test
    void handlesEmptyLists() {
        Task task = taskWith(new HashMap<>(Map.of(
                "graphFacts", List.of(),
                "graphRelations", List.of(),
                "vectorDocs", List.of()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        Map<String, Object> mergedContext =
                (Map<String, Object>) result.getOutputData().get("mergedContext");
        assertEquals(0, mergedContext.get("totalSources"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
