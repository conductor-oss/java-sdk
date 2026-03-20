package emailagent.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DraftEmailWorkerTest {

    private final DraftEmailWorker worker = new DraftEmailWorker();

    @Test
    void taskDefName() {
        assertEquals("ea_draft_email", worker.getTaskDefName());
    }

    @Test
    void returnsSubjectContainingProjectAlpha() {
        Task task = taskWith(Map.of(
                "emailType", "project_update",
                "keyPoints", List.of("Milestone 3 done"),
                "recipient", "sarah@company.com",
                "desiredTone", "professional"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String subject = (String) result.getOutputData().get("subject");
        assertNotNull(subject);
        assertTrue(subject.contains("Project Alpha"));
    }

    @Test
    void returnsMultiLineBody() {
        Task task = taskWith(Map.of("emailType", "project_update"));
        TaskResult result = worker.execute(task);

        String body = (String) result.getOutputData().get("body");
        assertNotNull(body);
        assertTrue(body.contains("\n"));
        assertTrue(body.contains("Dear Sarah"));
    }

    @Test
    void returnsWordCount() {
        Task task = taskWith(Map.of("emailType", "project_update"));
        TaskResult result = worker.execute(task);

        assertEquals(95, result.getOutputData().get("wordCount"));
    }

    @Test
    void bodyContainsMilestone3() {
        Task task = taskWith(Map.of("emailType", "project_update"));
        TaskResult result = worker.execute(task);

        String body = (String) result.getOutputData().get("body");
        assertTrue(body.contains("Milestone 3"));
    }

    @Test
    void handlesNullEmailType() {
        Map<String, Object> input = new HashMap<>();
        input.put("emailType", null);
        input.put("recipient", "sarah@company.com");
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subject"));
    }

    @Test
    void handlesBlankDesiredTone() {
        Task task = taskWith(Map.of("emailType", "update", "desiredTone", "  "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("body"));
    }

    @Test
    void handlesMissingInput() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("subject"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
