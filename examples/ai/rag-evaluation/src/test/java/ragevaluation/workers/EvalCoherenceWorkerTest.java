package ragevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvalCoherenceWorkerTest {

    private final EvalCoherenceWorker worker = new EvalCoherenceWorker();

    @Test
    void taskDefName() {
        assertEquals("re_eval_coherence", worker.getTaskDefName());
    }

    @Test
    void returnsCoherenceMetric() {
        Task task = taskWith(new HashMap<>(Map.of(
                "answer", "RAG pipelines combine retrieval and generation."
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("coherence", result.getOutputData().get("metric"));
        assertEquals(0.95, result.getOutputData().get("score"));
    }

    @Test
    void returnsReason() {
        Task task = taskWith(new HashMap<>(Map.of(
                "answer", "RAG pipelines combine retrieval and generation."
        )));
        TaskResult result = worker.execute(task);

        String reason = (String) result.getOutputData().get("reason");
        assertNotNull(reason);
        assertFalse(reason.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
