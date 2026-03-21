package ragpgvector.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PgvecGenerateWorkerTest {

    private final PgvecGenerateWorker worker = new PgvecGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("pgvec_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromRows() {
        List<Map<String, Object>> rows = List.of(
                new HashMap<>(Map.of("id", 1, "content", "pgvector adds vector similarity search.", "source", "pg_docs.md", "similarity", 0.94)),
                new HashMap<>(Map.of("id", 2, "content", "Create indexes with ivfflat or hnsw.", "source", "indexing.md", "similarity", 0.89)),
                new HashMap<>(Map.of("id", 3, "content", "Supports L2 distance, inner product, and cosine.", "source", "operators.md", "similarity", 0.86))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does pgvector handle similarity search?",
                "rows", rows
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("pgvector"));
        assertTrue(answer.contains("<=>"));
        assertTrue(answer.contains("<->"));
        assertTrue(answer.contains("<#>"));
        assertTrue(answer.contains("3 results"));
    }

    @Test
    void handlesEmptyRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "rows", List.of()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 results"));
    }

    @Test
    void handlesMissingRows() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 results"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>(Map.of(
                "rows", List.of(new HashMap<>(Map.of("id", 1, "content", "test")))
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("1 results"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
