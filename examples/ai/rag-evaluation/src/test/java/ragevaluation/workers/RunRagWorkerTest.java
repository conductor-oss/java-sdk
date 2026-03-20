package ragevaluation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RunRagWorkerTest {

    private final RunRagWorker worker = new RunRagWorker();

    @Test
    void taskDefName() {
        assertEquals("re_run_rag", worker.getTaskDefName());
    }

    @Test
    void returnsAnswer() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How do RAG pipelines work?")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertFalse(answer.isEmpty());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsContextWith3Passages() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How do RAG pipelines work?")));
        TaskResult result = worker.execute(task);

        List<String> context = (List<String>) result.getOutputData().get("context");
        assertNotNull(context);
        assertEquals(3, context.size());
        for (String passage : context) {
            assertFalse(passage.isEmpty());
        }
    }

    @Test
    void returnsRetrievedDocsCount() {
        Task task = taskWith(new HashMap<>(Map.of("question", "How do RAG pipelines work?")));
        TaskResult result = worker.execute(task);

        assertEquals(3, result.getOutputData().get("retrievedDocs"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
