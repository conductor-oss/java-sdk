package ragelasticsearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EsGenerateWorkerTest {

    private final EsGenerateWorker worker = new EsGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("es_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromHits() {
        List<Map<String, Object>> hits = List.of(
                Map.of("_id", "es-doc-1", "_score", 0.97,
                        "_source", Map.of("title", "ES Vector Search", "content", "test content 1")),
                Map.of("_id", "es-doc-2", "_score", 0.92,
                        "_source", Map.of("title", "Index Mapping", "content", "test content 2")),
                Map.of("_id", "es-doc-3", "_score", 0.88,
                        "_source", Map.of("title", "Hybrid Search", "content", "test content 3"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does Elasticsearch vector search work?",
                "hits", hits
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("knn search"));
        assertTrue(answer.contains("dense_vector"));
        assertTrue(answer.contains("Found 3 relevant docs"));
    }

    @Test
    void outputIsDeterministic() {
        List<Map<String, Object>> hits = List.of(
                Map.of("_id", "doc-1", "_score", 0.9, "_source", Map.of("title", "Test"))
        );

        Task task1 = taskWith(new HashMap<>(Map.of("question", "test", "hits", hits)));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "test", "hits", hits)));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("answer"), result2.getOutputData().get("answer"));
    }

    @Test
    void handlesNullHits() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question"
        )));
        // hits will be null since not in map
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Found 0 relevant docs"));
    }

    @Test
    void answerReflectsHitCount() {
        List<Map<String, Object>> hits = List.of(
                Map.of("_id", "doc-1", "_score", 0.95, "_source", Map.of("title", "Doc1"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "hits", hits
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Found 1 relevant docs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
