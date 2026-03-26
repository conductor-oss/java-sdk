package ragevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class EvalFaithfulnessWorkerTest {

    private final EvalFaithfulnessWorker worker = new EvalFaithfulnessWorker();

    @Test
    void taskDefName() {
        assertEquals("re_eval_faithfulness", worker.getTaskDefName());
    }

    @Test
    void returnsFaithfulnessMetric() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do RAG pipelines work?",
                "answer", "RAG pipelines combine retrieval and generation.",
                "context", List.of("RAG enhances LLM outputs.", "Retrieval uses vector search.")
        )));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("faithfulness", result.getOutputData().get("metric"));
        assertEquals(0.92, result.getOutputData().get("score"));
    }

    @Test
    void returnsReason() {
        Task task = taskWith(new HashMap<>(Map.of(
                "question", "How do RAG pipelines work?",
                "answer", "RAG pipelines combine retrieval and generation.",
                "context", List.of("RAG enhances LLM outputs.")
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
