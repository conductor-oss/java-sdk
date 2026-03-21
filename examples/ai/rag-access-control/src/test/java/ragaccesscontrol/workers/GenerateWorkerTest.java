package ragaccesscontrol.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GenerateWorkerTest {

    private final GenerateWorker worker = new GenerateWorker();

    @Test
    void taskDefName() {
        assertEquals("ac_generate", worker.getTaskDefName());
    }

    @Test
    void generatesAnswerWithoutRedaction() {
        List<Map<String, Object>> context = List.of(
                Map.of("id", "doc1", "content", "Company overview and mission statement."),
                Map.of("id", "doc2", "content", "Engineering guidelines for code review.")
        );
        Task task = taskWith(Map.of(
                "question", "What are the engineering guidelines?",
                "context", context,
                "userId", "user-42"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertNotNull(answer);
        assertTrue(answer.contains("What are the engineering guidelines?"));
        assertFalse(answer.contains("redacted"));

        assertEquals(2, result.getOutputData().get("sourcesUsed"));
        assertEquals(false, result.getOutputData().get("hasRedactedContent"));
    }

    @Test
    void notesRedactedContent() {
        List<Map<String, Object>> context = List.of(
                Map.of("id", "doc3", "content", "Benefits info. SSN: [SSN REDACTED]. Salary: [SALARY REDACTED].")
        );
        Task task = taskWith(Map.of(
                "question", "What are the benefits?",
                "context", context,
                "userId", "user-42"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());

        String answer = (String) result.getOutputData().get("answer");
        assertTrue(answer.contains("redacted"));
        assertEquals(true, result.getOutputData().get("hasRedactedContent"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
