package ragembeddingselection.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class PrepareBenchmarkWorkerTest {

    private final PrepareBenchmarkWorker worker = new PrepareBenchmarkWorker();

    @Test
    void taskDefName() {
        assertEquals("es_prepare_benchmark", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsBenchmarkQueries() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> queries = (List<Map<String, Object>>) result.getOutputData().get("benchmarkQueries");
        assertNotNull(queries);
        assertEquals(3, queries.size());
        assertEquals("q1", queries.get(0).get("id"));
        assertNotNull(queries.get(0).get("text"));
        assertNotNull(queries.get(0).get("expectedDocIds"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsBenchmarkCorpus() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> corpus = (List<Map<String, Object>>) result.getOutputData().get("benchmarkCorpus");
        assertNotNull(corpus);
        assertEquals(5, corpus.size());
        assertEquals("doc1", corpus.get(0).get("id"));
        assertNotNull(corpus.get(0).get("text"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsGroundTruth() {
        Task task = taskWith(new HashMap<>());
        TaskResult result = worker.execute(task);

        Map<String, List<String>> groundTruth = (Map<String, List<String>>) result.getOutputData().get("groundTruth");
        assertNotNull(groundTruth);
        assertEquals(3, groundTruth.size());
        assertTrue(groundTruth.containsKey("q1"));
        assertTrue(groundTruth.containsKey("q2"));
        assertTrue(groundTruth.containsKey("q3"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
