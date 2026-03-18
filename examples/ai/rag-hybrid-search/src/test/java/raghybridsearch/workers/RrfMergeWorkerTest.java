package raghybridsearch.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RrfMergeWorkerTest {

    private final RrfMergeWorker worker = new RrfMergeWorker();

    @Test
    void taskDefName() {
        assertEquals("hs_rrf_merge", worker.getTaskDefName());
    }

    @Test
    void deduplicatesOverlappingResults() {
        List<Map<String, Object>> vectorResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-7", "score", 0.92, "text", "Doc 7 text")),
                new HashMap<>(Map.of("id", "doc-3", "score", 0.87, "text", "Doc 3 text")),
                new HashMap<>(Map.of("id", "doc-12", "score", 0.81, "text", "Doc 12 text"))
        ));
        List<Map<String, Object>> keywordResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-7", "score", 4.2, "text", "Doc 7 text")),
                new HashMap<>(Map.of("id", "doc-19", "score", 3.8, "text", "Doc 19 text")),
                new HashMap<>(Map.of("id", "doc-5", "score", 3.1, "text", "Doc 5 text"))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "vectorResults", vectorResults,
                "keywordResults", keywordResults
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<String> mergedDocs = (List<String>) result.getOutputData().get("mergedDocs");
        assertNotNull(mergedDocs);
        // 3 vector + 3 keyword - 1 duplicate (doc-7) = 5 unique
        assertEquals(5, mergedDocs.size());
        assertEquals(5, result.getOutputData().get("uniqueCount"));
    }

    @Test
    void preservesOrderVectorFirst() {
        List<Map<String, Object>> vectorResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-7", "score", 0.92, "text", "Vector doc 7")),
                new HashMap<>(Map.of("id", "doc-3", "score", 0.87, "text", "Doc 3 text"))
        ));
        List<Map<String, Object>> keywordResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-7", "score", 4.2, "text", "Keyword doc 7")),
                new HashMap<>(Map.of("id", "doc-19", "score", 3.8, "text", "Doc 19 text"))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "vectorResults", vectorResults,
                "keywordResults", keywordResults
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> mergedDocs = (List<String>) result.getOutputData().get("mergedDocs");

        // doc-7 text should come from vector (first seen)
        assertEquals("Vector doc 7", mergedDocs.get(0));
        assertEquals("Doc 3 text", mergedDocs.get(1));
        assertEquals("Doc 19 text", mergedDocs.get(2));
        assertEquals(3, mergedDocs.size());
    }

    @Test
    void handlesNoOverlap() {
        List<Map<String, Object>> vectorResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-1", "score", 0.9, "text", "Text 1"))
        ));
        List<Map<String, Object>> keywordResults = new ArrayList<>(List.of(
                new HashMap<>(Map.of("id", "doc-2", "score", 3.0, "text", "Text 2"))
        ));

        Task task = taskWith(new HashMap<>(Map.of(
                "vectorResults", vectorResults,
                "keywordResults", keywordResults
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> mergedDocs = (List<String>) result.getOutputData().get("mergedDocs");
        assertEquals(2, mergedDocs.size());
        assertEquals(2, result.getOutputData().get("uniqueCount"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
