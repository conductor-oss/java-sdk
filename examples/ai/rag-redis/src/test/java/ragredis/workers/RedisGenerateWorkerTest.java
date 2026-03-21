package ragredis.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RedisGenerateWorkerTest {

    private final RedisGenerateWorker worker = new RedisGenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("redis_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerFromResults() {
        List<Map<String, Object>> results = List.of(
                new HashMap<>(Map.of("key", "doc:1001", "content", "Some content", "vector_score", 0.04)),
                new HashMap<>(Map.of("key", "doc:1042", "content", "More content", "vector_score", 0.09)),
                new HashMap<>(Map.of("key", "doc:1078", "content", "Even more", "vector_score", 0.14))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How does Redis handle vector search?",
                "results", results
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("Found 3 results."));
        assertTrue(answer.contains("RediSearch"));
        assertTrue(answer.contains("FT.SEARCH"));
        assertTrue(answer.contains("KNN"));
        assertTrue(answer.contains("DIALECT 2"));
    }

    @Test
    void answerContainsCorrectResultCount() {
        List<Map<String, Object>> results = List.of(
                new HashMap<>(Map.of("key", "doc:1", "content", "A")),
                new HashMap<>(Map.of("key", "doc:2", "content", "B"))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "results", results
        )));
        TaskResult result = worker.execute(task);

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Found 2 results."));
    }

    @Test
    void handlesNullResults() {
        Map<String, Object> input = new HashMap<>();
        input.put("question", "test");
        input.put("results", null);

        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("Found 0 results."));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
