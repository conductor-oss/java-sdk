package ragreranking.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RetrieveWorkerTest {

    private final RetrieveWorker worker = new RetrieveWorker();

    @Test
    void taskDefName() {
        assertEquals("rerank_retrieve", worker.getTaskDefName());
    }

    @Test
    void returnsSixCandidates() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1, -0.3, 0.5),
                "topK", 6
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) result.getOutputData().get("candidates");
        assertNotNull(candidates);
        assertEquals(6, candidates.size());
    }

    @Test
    void candidatesHaveExpectedIds() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "topK", 6
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) result.getOutputData().get("candidates");

        assertEquals("doc-A", candidates.get(0).get("id"));
        assertEquals("doc-B", candidates.get(1).get("id"));
        assertEquals("doc-C", candidates.get(2).get("id"));
        assertEquals("doc-D", candidates.get(3).get("id"));
        assertEquals("doc-E", candidates.get(4).get("id"));
        assertEquals("doc-F", candidates.get(5).get("id"));
    }

    @Test
    void candidatesHaveBiEncoderScores() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "topK", 6
        )));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> candidates =
                (List<Map<String, Object>>) result.getOutputData().get("candidates");

        assertEquals(0.91, candidates.get(0).get("biEncoderScore"));
        assertEquals(0.88, candidates.get(1).get("biEncoderScore"));
        assertEquals(0.86, candidates.get(2).get("biEncoderScore"));
        assertEquals(0.82, candidates.get(3).get("biEncoderScore"));
        assertEquals(0.79, candidates.get(4).get("biEncoderScore"));
        assertEquals(0.76, candidates.get(5).get("biEncoderScore"));
    }

    @Test
    void returnsCandidateCount() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "topK", 6
        )));
        TaskResult result = worker.execute(task);

        assertEquals(6, result.getOutputData().get("candidateCount"));
    }

    @Test
    void parsesStringTopK() {
        Task task = taskWith(new HashMap<>(Map.of(
                "embedding", List.of(0.1),
                "topK", "6"
        )));
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
