package ragcode.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateCodeAnswerWorkerTest {

    private final GenerateCodeAnswerWorker worker = new GenerateCodeAnswerWorker();

    @Test
    void taskDefName() {
        assertEquals("cr_generate_code_answer", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithSnippets() {
        List<Map<String, Object>> codeSnippets = List.of(
                new HashMap<>(Map.of(
                        "id", "snippet-001",
                        "file", "src/main/java/com/example/UserService.java",
                        "line", 42,
                        "signature", "public User findUserById(String userId)",
                        "body", "public User findUserById(String userId) { return repo.find(userId); }",
                        "astType", "method_definition",
                        "score", 0.95
                )),
                new HashMap<>(Map.of(
                        "id", "snippet-002",
                        "file", "src/main/java/com/example/OrderController.java",
                        "line", 78,
                        "signature", "public ResponseEntity<Order> createOrder(OrderRequest request)",
                        "body", "public ResponseEntity<Order> createOrder(OrderRequest request) { return ok(); }",
                        "astType", "method_definition",
                        "score", 0.89
                ))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do I look up a user by ID?",
                "codeSnippets", codeSnippets
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("2 code snippets"));
        assertTrue(answer.contains("findUserById"));
        assertTrue(answer.contains("UserService.java"));
    }

    @Test
    void codeExampleFromTopSnippet() {
        String body = "public User findUserById(String userId) { return repo.find(userId); }";
        List<Map<String, Object>> codeSnippets = List.of(
                new HashMap<>(Map.of(
                        "id", "snippet-001",
                        "file", "UserService.java",
                        "line", 10,
                        "signature", "public User findUserById(String userId)",
                        "body", body,
                        "astType", "method_definition",
                        "score", 0.95
                ))
        );

        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test",
                "codeSnippets", codeSnippets
        )));
        TaskResult result = worker.execute(task);

        assertEquals(body, result.getOutputData().get("codeExample"));
    }

    @Test
    void handlesEmptySnippets() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do I do X?",
                "codeSnippets", List.of()
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No matching code snippets"));
        assertTrue(answer.contains("How do I do X?"));

        String codeExample = (String) result.getOutputData().get("codeExample");
        assertEquals("// No code example available", codeExample);
    }

    @Test
    void handlesNullSnippets() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "test query"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("No matching code snippets"));
        assertEquals("// No code example available", result.getOutputData().get("codeExample"));
    }

    @Test
    void handlesMissingQuestion() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("answer"));
        assertNotNull(result.getOutputData().get("codeExample"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
