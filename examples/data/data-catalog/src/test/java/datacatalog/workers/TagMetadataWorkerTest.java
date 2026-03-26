package datacatalog.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class TagMetadataWorkerTest {

    private final TagMetadataWorker worker = new TagMetadataWorker();

    @Test
    void taskDefName() {
        assertEquals("cg_tag_metadata", worker.getTaskDefName());
    }

    @Test
    void returnsCompletedStatus() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "operational", "type", "table", "sensitivity", "low", "hasPII", false, "rowCount", 100))));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsBasicTags() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "operational", "type", "table", "sensitivity", "low", "hasPII", false, "rowCount", 100))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> tagged = (List<Map<String, Object>>) result.getOutputData().get("tagged");
        List<String> tags = (List<String>) tagged.get(0).get("tags");
        assertTrue(tags.contains("operational"));
        assertTrue(tags.contains("table"));
        assertTrue(tags.contains("low"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsPIITags() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "operational", "type", "table", "sensitivity", "high", "hasPII", true, "rowCount", 100))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> tagged = (List<Map<String, Object>>) result.getOutputData().get("tagged");
        List<String> tags = (List<String>) tagged.get(0).get("tags");
        assertTrue(tags.contains("pii"));
        assertTrue(tags.contains("gdpr-relevant"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void addsLargeDatasetTag() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "analytics", "type", "table", "sensitivity", "high", "hasPII", true, "rowCount", 1000000))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> tagged = (List<Map<String, Object>>) result.getOutputData().get("tagged");
        List<String> tags = (List<String>) tagged.get(0).get("tags");
        assertTrue(tags.contains("large-dataset"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void noLargeDatasetTagForSmallTables() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "operational", "type", "table", "sensitivity", "low", "hasPII", false, "rowCount", 5000))));
        TaskResult result = worker.execute(task);
        List<Map<String, Object>> tagged = (List<Map<String, Object>>) result.getOutputData().get("tagged");
        List<String> tags = (List<String>) tagged.get(0).get("tags");
        assertFalse(tags.contains("large-dataset"));
    }

    @Test
    void tagCountAccumulated() {
        Task task = taskWith(Map.of("classifiedAssets", List.of(
                Map.of("name", "t1", "category", "operational", "type", "table", "sensitivity", "low", "hasPII", false, "rowCount", 100),
                Map.of("name", "t2", "category", "analytics", "type", "table", "sensitivity", "high", "hasPII", true, "rowCount", 200000))));
        TaskResult result = worker.execute(task);
        int tagCount = (int) result.getOutputData().get("tagCount");
        // t1: operational, table, low = 3 tags
        // t2: analytics, table, high, pii, gdpr-relevant, large-dataset = 6 tags
        assertEquals(9, tagCount);
    }

    @Test
    void handlesNullAssets() {
        Map<String, Object> input = new HashMap<>();
        input.put("classifiedAssets", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(0, result.getOutputData().get("tagCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
