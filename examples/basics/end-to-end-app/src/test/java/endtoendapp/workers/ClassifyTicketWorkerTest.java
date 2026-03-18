package endtoendapp.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClassifyTicketWorkerTest {

    private final ClassifyTicketWorker worker = new ClassifyTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("classify_ticket", worker.getTaskDefName());
    }

    @Test
    void classifiesCriticalCorrectly() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-100",
                "subject", "Production server is down",
                "description", "Everything in production is broken",
                "category", "technical"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("CRITICAL", result.getOutputData().get("priority"));
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywordsMatched");
        assertTrue(keywords.contains("production"));
    }

    @Test
    void classifiesLowCorrectly() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-101",
                "subject", "Question about my account",
                "description", "How do I update my profile?",
                "category", "general"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("LOW", result.getOutputData().get("priority"));
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywordsMatched");
        assertTrue(keywords.contains("question"));
    }

    @Test
    void handlesUnknownKeywordsAsLow() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-102",
                "subject", "Something random",
                "description", "No matching keywords here at all",
                "category", "general"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("LOW", result.getOutputData().get("priority"));
        @SuppressWarnings("unchecked")
        List<String> keywords = (List<String>) result.getOutputData().get("keywordsMatched");
        assertTrue(keywords.isEmpty());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
