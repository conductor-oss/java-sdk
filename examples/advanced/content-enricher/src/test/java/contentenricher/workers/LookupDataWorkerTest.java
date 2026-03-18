package contentenricher.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class LookupDataWorkerTest {

    private final LookupDataWorker worker = new LookupDataWorker();

    @Test
    void taskDefName() {
        assertEquals("enr_lookup_data", worker.getTaskDefName());
    }

    @Test
    void returnsLookupDataWithoutUrl() {
        Task task = taskWith(Map.of("customerId", "CUST-42"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("lookupData"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lookupDataContainsCustomerId() {
        Task task = taskWith(Map.of("customerId", "CUST-42"));
        TaskResult result = worker.execute(task);

        Map<String, Object> lookup = (Map<String, Object>) result.getOutputData().get("lookupData");
        assertEquals("CUST-42", lookup.get("customerId"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void lookupDataHasMetadataFields() {
        Task task = taskWith(Map.of("customerId", "CUST-1"));
        TaskResult result = worker.execute(task);

        Map<String, Object> lookup = (Map<String, Object>) result.getOutputData().get("lookupData");
        assertTrue(lookup.containsKey("title"), "Should contain title");
        assertTrue(lookup.containsKey("description"), "Should contain description");
        assertTrue(lookup.containsKey("wordCount"), "Should contain wordCount");
        assertTrue(lookup.containsKey("ogTitle"), "Should contain ogTitle");
    }

    @SuppressWarnings("unchecked")
    @Test
    void parseHtmlExtractsTitle() {
        String html = "<html><head><title>Test Page</title></head><body>Hello world</body></html>";
        Map<String, Object> metadata = LookupDataWorker.parseHtmlMetadata(html);

        assertEquals("Test Page", metadata.get("title"));
        assertTrue((int) metadata.get("wordCount") > 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    void parseHtmlExtractsOgTags() {
        String html = "<html><head>"
                + "<meta property=\"og:title\" content=\"OG Title Here\"/>"
                + "<meta property=\"og:description\" content=\"OG Desc\"/>"
                + "<meta property=\"og:image\" content=\"https://example.com/img.png\"/>"
                + "</head><body>Content</body></html>";
        Map<String, Object> metadata = LookupDataWorker.parseHtmlMetadata(html);

        assertEquals("OG Title Here", metadata.get("ogTitle"));
        assertEquals("OG Desc", metadata.get("ogDescription"));
        assertEquals("https://example.com/img.png", metadata.get("ogImage"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void parseHtmlCountsWords() {
        String html = "<html><body><p>one two three four five</p></body></html>";
        Map<String, Object> metadata = LookupDataWorker.parseHtmlMetadata(html);

        int wc = (int) metadata.get("wordCount");
        assertTrue(wc >= 5, "Word count should be at least 5, got: " + wc);
    }

    @Test
    void handlesNullCustomerId() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("lookupData"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
