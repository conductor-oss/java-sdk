package agentsupervisor.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TesterAgentWorkerTest {

    private final TesterAgentWorker worker = new TesterAgentWorker();

    @Test
    void taskDefName() {
        assertEquals("sup_tester_agent", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsTestResults() {
        Task task = taskWith(new HashMap<>(Map.of(
                "task", "Create test suites for user-authentication",
                "feature", "user-authentication",
                "systemPrompt", "You are a tester agent."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(testResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsTestSuiteMetrics() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(testResult.get("testSuites"));
        assertTrue(((Number) testResult.get("testSuites")).intValue() > 0);
        assertTrue(((Number) testResult.get("totalTests")).intValue() > 0);
        assertTrue(((Number) testResult.get("passed")).intValue() >= 0);
        assertTrue(((Number) testResult.get("failed")).intValue() >= 0);
    }

    @Test
    @SuppressWarnings("unchecked")
    void reportsCoveragePercentage() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        String coverage = (String) testResult.get("coverage");
        assertNotNull(coverage);
        assertTrue(coverage.endsWith("%"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void identifiesFailingTest() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(testResult.get("failedTest"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void statusReflectsTestOutcome() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        String status = (String) testResult.get("status");
        assertTrue(status.equals("needs_fix") || status.equals("all_passed"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenMissing() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(testResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    void usesDefaultFeatureWhenNull() {
        Map<String, Object> input = new HashMap<>();
        input.put("feature", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertNotNull(testResult);
    }

    @Test
    @SuppressWarnings("unchecked")
    void passedPlusFailedEqualsTotalTests() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        int total = ((Number) testResult.get("totalTests")).intValue();
        int passed = ((Number) testResult.get("passed")).intValue();
        int failed = ((Number) testResult.get("failed")).intValue();
        assertEquals(total, passed + failed);
    }

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "test-feature")));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
        @SuppressWarnings("unchecked")
        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        assertTrue(testResult.containsKey("testSuites"));
        assertTrue(testResult.containsKey("totalTests"));
        assertTrue(testResult.containsKey("passed"));
        assertTrue(testResult.containsKey("failed"));
        assertTrue(testResult.containsKey("coverage"));
        assertTrue(testResult.containsKey("failedTest"));
        assertTrue(testResult.containsKey("status"));
        assertTrue(testResult.containsKey("testCode"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void generatesTestsFromCodeInput() {
        String code = """
                public class PaymentService {
                    public Map<String, Object> processPayment(Map<String, Object> data) {
                        return Map.of();
                    }
                    public void refund(String transactionId) {
                    }
                }
                """;
        Task task = taskWith(new HashMap<>(Map.of("feature", "payments", "code", code)));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        String testCode = (String) testResult.get("testCode");
        assertNotNull(testCode);
        assertTrue(testCode.contains("PaymentServiceTest"));
        assertTrue(testCode.contains("@Test"));
        assertTrue(testCode.contains("testProcessPayment"));
        assertTrue(testCode.contains("testRefund"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testMethodsListIsPopulated() {
        Task task = taskWith(new HashMap<>(Map.of("feature", "user-authentication")));
        TaskResult result = worker.execute(task);

        Map<String, Object> testResult = (Map<String, Object>) result.getOutputData().get("result");
        List<String> testMethods = (List<String>) testResult.get("testMethods");
        assertNotNull(testMethods);
        assertFalse(testMethods.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
