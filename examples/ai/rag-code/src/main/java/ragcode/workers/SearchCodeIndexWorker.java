package ragcode.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker that searches a code index using the embedding vector and code filter.
 * Returns 3 fixed code snippets with id, file, line, signature, body, astType, and score.
 * In production this would query a code search index or vector database.
 */
public class SearchCodeIndexWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_search_code_index";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [search_code_index] Searching code index for matching snippets");

        // Fixed deterministic code search results
        List<Map<String, Object>> codeSnippets = List.of(
                Map.of(
                        "id", "snippet-001",
                        "file", "src/main/java/com/example/UserService.java",
                        "line", 42,
                        "signature", "public User findUserById(String userId)",
                        "body", "public User findUserById(String userId) {\n    return userRepository.findById(userId)\n        .orElseThrow(() -> new UserNotFoundException(userId));\n}",
                        "astType", "method_definition",
                        "score", 0.95
                ),
                Map.of(
                        "id", "snippet-002",
                        "file", "src/main/java/com/example/OrderController.java",
                        "line", 78,
                        "signature", "public ResponseEntity<Order> createOrder(OrderRequest request)",
                        "body", "public ResponseEntity<Order> createOrder(OrderRequest request) {\n    Order order = orderService.create(request);\n    return ResponseEntity.status(HttpStatus.CREATED).body(order);\n}",
                        "astType", "method_definition",
                        "score", 0.89
                ),
                Map.of(
                        "id", "snippet-003",
                        "file", "src/main/java/com/example/CacheUtil.java",
                        "line", 15,
                        "signature", "public static <T> T getOrCompute(String key, Supplier<T> supplier)",
                        "body", "public static <T> T getOrCompute(String key, Supplier<T> supplier) {\n    T cached = cache.get(key);\n    if (cached != null) return cached;\n    T value = supplier.get();\n    cache.put(key, value);\n    return value;\n}",
                        "astType", "method_definition",
                        "score", 0.82
                )
        );

        System.out.println("  [search_code_index] Found " + codeSnippets.size() + " code snippets");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("codeSnippets", codeSnippets);
        return result;
    }
}
