package ragaccesscontrol.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FilteredRetrieveWorkerTest {

    private final FilteredRetrieveWorker worker = new FilteredRetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_filtered_retrieve", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void filtersToAllowedCollections() {
        Task task = taskWith(Map.of(
                "question", "What are the engineering guidelines?",
                "allowedCollections", List.of("public-docs", "engineering-wiki", "hr-policies"),
                "clearanceLevel", "confidential"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertNotNull(docs);
        assertEquals(3, docs.size());

        for (Map<String, Object> doc : docs) {
            String collection = (String) doc.get("collection");
            assertTrue(List.of("public-docs", "engineering-wiki", "hr-policies").contains(collection));
        }

        assertEquals(5, result.getOutputData().get("totalRetrieved"));
        assertEquals(3, result.getOutputData().get("afterFilter"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void publicOnlyAccess() {
        Task task = taskWith(Map.of(
                "question", "General question",
                "allowedCollections", List.of("public-docs"),
                "clearanceLevel", "public"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> docs = (List<Map<String, Object>>) result.getOutputData().get("documents");
        assertEquals(1, docs.size());
        assertEquals("public-docs", docs.get(0).get("collection"));
        assertEquals(5, result.getOutputData().get("totalRetrieved"));
        assertEquals(1, result.getOutputData().get("afterFilter"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
