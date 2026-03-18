package agentsupervisor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CoderAgentWorkerTest {

    private final CoderAgentWorker worker = new CoderAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("sup_coder_agent", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsImplementationResult() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement user-authentication with all required endpoints",
                "feature", "user-authentication",
                "systemPrompt", "You are a coder agent."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(codeResult);
        assertEquals("implemented", codeResult.get("status"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void createsThreeFiles() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement user-authentication",
                "feature", "user-authentication"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> filesCreated = (List<String>) codeResult.get("filesCreated");
        assertNotNull(filesCreated);
        assertEquals(3, filesCreated.size());
        assertTrue(filesCreated.stream().anyMatch(f -> f.contains("Controller")));
        assertTrue(filesCreated.stream().anyMatch(f -> f.contains("Service")));
        assertTrue(filesCreated.stream().anyMatch(f -> f.contains("Repository")));
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsLinesOfCodeAndLanguage() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement feature",
                "feature", "user-authentication"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        assertInstanceOf(Integer.class, codeResult.get("linesOfCode"));
        assertTrue(((Integer) codeResult.get("linesOfCode")) > 0);
        assertEquals("Java", codeResult.get("language"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void notesContainFeatureName() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement payment-processing",
                "feature", "payment-processing"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        String notes = (String) codeResult.get("notes");
        assertTrue(notes.contains("payment-processing"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatedCodeContainsProperPackageAndClass() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement auth",
                "feature", "user-authentication"
        )));
        TaskResult result = worker.execute(task);

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        Map<String, String> code = (Map<String, String>) codeResult.get("code");
        assertNotNull(code);
        assertFalse(code.isEmpty());

        // At least one file should contain a proper Java class
        boolean hasClassDecl = code.values().stream()
                .anyMatch(c -> c.contains("class UserAuthentication"));
        assertTrue(hasClassDecl);
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        String notes = (String) codeResult.get("notes");
        assertTrue(notes.contains("user-authentication"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("feature", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        String notes = (String) codeResult.get("notes");
        assertTrue(notes.contains("user-authentication"));
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Implement feature",
                "feature", "test-feature"
        )));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        assertTrue(codeResult.containsKey("filesCreated"));
        assertTrue(codeResult.containsKey("linesOfCode"));
        assertTrue(codeResult.containsKey("language"));
        assertTrue(codeResult.containsKey("status"));
        assertTrue(codeResult.containsKey("notes"));
        assertTrue(codeResult.containsKey("code"));
    }

    @Test
    void sameInputProducesSameOutput() {
        Task task1 = taskWith(new HashMap<>(Map.of("feature", "search")));
        Task task2 = taskWith(new HashMap<>(Map.of("feature", "search")));

        TaskResult r1 = worker.execute(task1);
        TaskResult r2 = worker.execute(task2);

        assertEquals(r1.getOutputData().get("result"), r2.getOutputData().get("result"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void controllerCodeContainsMethodSignatures() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "order-management")));
        TaskResult result = worker.execute(task);

        Map<String, Object> codeResult = (Map<String, Object>) result.getOutputData().get("result");
        Map<String, String> code = (Map<String, String>) codeResult.get("code");

        // Find the controller source code
        String controllerCode = code.entrySet().stream()
                .filter(e -> e.getKey().contains("Controller"))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse("");

        assertTrue(controllerCode.contains("createOrderManagement"));
        assertTrue(controllerCode.contains("getOrderManagement"));
        assertTrue(controllerCode.contains("updateOrderManagement"));
        assertTrue(controllerCode.contains("deleteOrderManagement"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
