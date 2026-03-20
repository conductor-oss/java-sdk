package incrementalrag.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilterNewDocsWorkerTest {

    private final FilterNewDocsWorker worker = new FilterNewDocsWorker();

    @Test
    void taskDefName() {
        assertEquals("ir_filter_new_docs", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void separatesNewAndUpdatedDocs() {
        Map<String, Object> existingHashes = new HashMap<>();
        existingHashes.put("doc-101", "a1b2c3");
        existingHashes.put("doc-205", null);
        existingHashes.put("doc-307", "d4e5f6");
        existingHashes.put("doc-412", null);

        Task task = taskWith(Map.of(
                "changedDocIds", List.of("doc-101", "doc-205", "doc-307", "doc-412"),
                "existingHashes", existingHashes
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, String>> docsToEmbed = (List<Map<String, String>>) result.getOutputData().get("docsToEmbed");
        assertNotNull(docsToEmbed);
        assertEquals(4, docsToEmbed.size());

        // doc-101 has a hash -> update
        assertEquals("doc-101", docsToEmbed.get(0).get("id"));
        assertEquals("update", docsToEmbed.get(0).get("action"));

        // doc-205 has null hash -> insert
        assertEquals("doc-205", docsToEmbed.get(1).get("id"));
        assertEquals("insert", docsToEmbed.get(1).get("action"));

        // doc-307 has a hash -> update
        assertEquals("doc-307", docsToEmbed.get(2).get("id"));
        assertEquals("update", docsToEmbed.get(2).get("action"));

        // doc-412 has null hash -> insert
        assertEquals("doc-412", docsToEmbed.get(3).get("id"));
        assertEquals("insert", docsToEmbed.get(3).get("action"));
    }

    @Test
    void returnsCounts() {
        Map<String, Object> existingHashes = new HashMap<>();
        existingHashes.put("doc-101", "a1b2c3");
        existingHashes.put("doc-205", null);
        existingHashes.put("doc-307", "d4e5f6");
        existingHashes.put("doc-412", null);

        Task task = taskWith(Map.of(
                "changedDocIds", List.of("doc-101", "doc-205", "doc-307", "doc-412"),
                "existingHashes", existingHashes
        ));
        TaskResult result = worker.execute(task);

        assertEquals(2, result.getOutputData().get("newCount"));
        assertEquals(2, result.getOutputData().get("updatedCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
