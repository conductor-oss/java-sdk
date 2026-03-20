package ragqdrant.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class QdrantGenerateWorkerTest {

    private final QdrantGenerateWorker worker = new QdrantGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("qdrant_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromPoints() {
        List<Map<String, Object>> points = new ArrayList<>();
        points.add(new HashMap<>(Map.of("id", "a1b2c3d4", "score", 0.96,
                "payload", new HashMap<>(Map.of("title", "Qdrant Overview", "content", "content1")))));
        points.add(new HashMap<>(Map.of("id", "e5f6a7b8", "score", 0.92,
                "payload", new HashMap<>(Map.of("title", "Payload Filtering", "content", "content2")))));
        points.add(new HashMap<>(Map.of("id", "c9d0e1f2", "score", 0.88,
                "payload", new HashMap<>(Map.of("title", "Collections", "content", "content3")))));

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does Qdrant filter during search?",
                "points", points
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("3 retrieved points"));
        assertTrue(answer.contains("vector similarity search engine"));
        assertTrue(answer.contains("HNSW indexing"));
    }

    @Test
    void handlesEmptyPoints() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test question",
                "points", new ArrayList<>()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("0 retrieved points"));
    }

    @Test
    void defaultsQuestionWhenMissing() {
        Task task = taskWith(new HashMap<>(Map.of(
                "points", new ArrayList<>()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
    }

    @Test
    void defaultsQuestionWhenBlank() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "   ",
                "points", new ArrayList<>()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    void answerIsDeterministic() {
        List<Map<String, Object>> points = new ArrayList<>();
        points.add(new HashMap<>(Map.of("id", "a1b2c3d4", "score", 0.96)));

        Task task1 = taskWith(new HashMap<>(Map.of("question", "q1", "points", points)));
        Task task2 = taskWith(new HashMap<>(Map.of("question", "q2", "points", points)));
        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("answer"),
                     result2.getOutputData().get("answer"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
