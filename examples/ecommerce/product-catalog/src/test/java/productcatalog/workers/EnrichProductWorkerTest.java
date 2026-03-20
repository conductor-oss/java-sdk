package productcatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EnrichProductWorkerTest {

    private final EnrichProductWorker worker = new EnrichProductWorker();

    @Test
    void taskDefName() {
        assertEquals("prd_enrich", worker.getTaskDefName());
    }

    @Test
    void generatesSlugFromName() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Running Shoes", "category", "Footwear"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        assertEquals("running-shoes", enriched.get("slug"));
    }

    @Test
    void generatesSeoTitle() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Widget", "category", "Tools"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        assertEquals("Buy Widget | Best Tools", enriched.get("seoTitle"));
    }

    @Test
    void includesCategoryInTags() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Lamp", "category", "Home"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        @SuppressWarnings("unchecked")
        List<String> tags = (List<String>) enriched.get("tags");
        assertTrue(tags.contains("Home"));
        assertTrue(tags.contains("new-arrival"));
        assertTrue(tags.contains("featured"));
    }

    @Test
    void handlesNullName() {
        Task task = taskWith(new HashMap<>(Map.of("productId", "prod-1", "category", "General")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        assertEquals("unknown", enriched.get("slug"));
    }

    @Test
    void handlesNullCategory() {
        Task task = taskWith(new HashMap<>(Map.of("productId", "prod-1", "name", "Test")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        assertTrue(enriched.get("seoTitle").toString().contains("general"));
    }

    @Test
    void slugHandlesMultipleSpaces() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Big  Red   Ball", "category", "Toys"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> enriched = (Map<String, Object>) result.getOutputData().get("enrichedData");
        assertEquals("big-red-ball", enriched.get("slug"));
    }

    @Test
    void outputContainsEnrichedDataKey() {
        Task task = taskWith(Map.of("productId", "prod-1", "name", "Item", "category", "Cat"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("enrichedData"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
