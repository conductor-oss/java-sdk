package llmchain.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChainValidateWorkerTest {

    private final ChainValidateWorker worker = new ChainValidateWorker();

    @Test
    void taskDefName() {
        assertEquals("chain_validate", worker.getTaskDefName());
    }

    @Test
    void allChecksPassWithValidData() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "complaint",
                "sentiment", "negative",
                "suggestedProducts", List.of("PROD-ENT-500", "PROD-SUPPORT-PREM"),
                "draftReply", "Dear Customer,\n\nThank you for reaching out. We sincerely apologize for the inconvenience you have experienced. Our team is committed to resolving your issue promptly."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-BASIC-100,PROD-PRO-250,PROD-ENT-500,PROD-SUPPORT-PREM,PROD-SUPPORT-STD"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("allChecksPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("checks");
        assertEquals(4, checks.size());
        for (Map<String, Object> check : checks) {
            assertTrue((Boolean) check.get("passed"),
                    "Check failed: " + check.get("rule"));
        }
    }

    @Test
    void failsWithInvalidIntent() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "unknown_intent",
                "sentiment", "neutral",
                "suggestedProducts", List.of(),
                "draftReply", "This is a sufficiently long reply that exceeds the fifty character minimum for validation."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("allChecksPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> intentCheck = checks.stream()
                .filter(c -> "valid_intent".equals(c.get("rule")))
                .findFirst().orElseThrow();
        assertFalse((Boolean) intentCheck.get("passed"));
    }

    @Test
    void failsWithInvalidSentiment() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "inquiry",
                "sentiment", "angry",
                "suggestedProducts", List.of(),
                "draftReply", "This is a sufficiently long reply that exceeds the fifty character minimum for validation."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("allChecksPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> sentimentCheck = checks.stream()
                .filter(c -> "valid_sentiment".equals(c.get("rule")))
                .findFirst().orElseThrow();
        assertFalse((Boolean) sentimentCheck.get("passed"));
    }

    @Test
    void failsWhenProductNotInCatalog() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "purchase",
                "sentiment", "positive",
                "suggestedProducts", List.of("PROD-UNKNOWN"),
                "draftReply", "This is a sufficiently long reply that exceeds the fifty character minimum for validation."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A,PROD-B"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("allChecksPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> productCheck = checks.stream()
                .filter(c -> "products_in_catalog".equals(c.get("rule")))
                .findFirst().orElseThrow();
        assertFalse((Boolean) productCheck.get("passed"));
    }

    @Test
    void failsWhenReplyTooShort() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "inquiry",
                "sentiment", "neutral",
                "suggestedProducts", List.of(),
                "draftReply", "Too short"
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(false, result.getOutputData().get("allChecksPassed"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> checks =
                (List<Map<String, Object>>) result.getOutputData().get("checks");
        Map<String, Object> lengthCheck = checks.stream()
                .filter(c -> "reply_length".equals(c.get("rule")))
                .findFirst().orElseThrow();
        assertFalse((Boolean) lengthCheck.get("passed"));
    }

    @Test
    void returnsValidatedResultMatchingInput() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "complaint",
                "sentiment", "negative",
                "suggestedProducts", List.of("PROD-A"),
                "draftReply", "This is a sufficiently long reply that exceeds the fifty character minimum for validation."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A,PROD-B"
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> validatedResult =
                (Map<String, Object>) result.getOutputData().get("validatedResult");
        assertEquals(parsedData, validatedResult);
    }

    @Test
    void handlesSpacesInCatalog() {
        Map<String, Object> parsedData = new HashMap<>(Map.of(
                "intent", "purchase",
                "sentiment", "positive",
                "suggestedProducts", List.of("PROD-A", "PROD-B"),
                "draftReply", "This is a sufficiently long reply that exceeds the fifty character minimum for validation."
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "parsedData", parsedData,
                "productCatalog", "PROD-A , PROD-B , PROD-C"
        )));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("allChecksPassed"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
