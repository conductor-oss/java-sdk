package enterpriserag.workers;

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
        assertEquals("er_retrieve", worker.getTaskDefName());
    }

    @Test
    @SuppressWarnings("unchecked")
    void returnsFourContextDocuments() {
        Task task = taskWith(Map.of("question", "What is RAG?"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        List<Map<String, Object>> context =
                (List<Map<String, Object>>) result.getOutputData().get("context");
        assertNotNull(context);
        assertEquals(4, context.size());
    }

    @Test
    @SuppressWarnings("unchecked")
    void documentsHaveRequiredFields() {
        Task task = taskWith(Map.of("question", "What is RAG?"));
        TaskResult result = worker.execute(task);

        List<Map<String, Object>> context =
                (List<Map<String, Object>>) result.getOutputData().get("context");
        for (Map<String, Object> doc : context) {
            assertNotNull(doc.get("id"));
            assertNotNull(doc.get("text"));
            assertNotNull(doc.get("tokens"));
            assertInstanceOf(Integer.class, doc.get("tokens"));
        }
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
