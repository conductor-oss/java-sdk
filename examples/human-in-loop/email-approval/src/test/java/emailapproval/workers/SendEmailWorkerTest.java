package emailapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SendEmailWorkerTest {

    @Test
    void taskDefName() {
        SendEmailWorker worker = new SendEmailWorker();
        assertEquals("ea_send_email", worker.getTaskDefName());
    }

    @Test
    void returnsSentTrueWithUrls() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>(Map.of("requester", "user@example.com")));
        task.setWorkflowInstanceId("wf-123");
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertNotNull(result.getOutputData().get("approveUrl"));
        assertNotNull(result.getOutputData().get("rejectUrl"));
    }

    @Test
    void approveUrlContainsWorkflowId() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId("wf-456");
        TaskResult result = worker.execute(task);

        String approveUrl = (String) result.getOutputData().get("approveUrl");
        assertTrue(approveUrl.contains("wf-456"));
        assertTrue(approveUrl.contains("action=approve"));
    }

    @Test
    void rejectUrlContainsWorkflowId() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId("wf-789");
        TaskResult result = worker.execute(task);

        String rejectUrl = (String) result.getOutputData().get("rejectUrl");
        assertTrue(rejectUrl.contains("wf-789"));
        assertTrue(rejectUrl.contains("action=reject"));
    }

    @Test
    void handlesNullWorkflowId() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId(null);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        String approveUrl = (String) result.getOutputData().get("approveUrl");
        assertTrue(approveUrl.contains("unknown"));
    }

    @Test
    void outputContainsAllExpectedKeys() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId("wf-keys");
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("sent"));
        assertTrue(result.getOutputData().containsKey("approveUrl"));
        assertTrue(result.getOutputData().containsKey("rejectUrl"));
    }

    @Test
    void urlsAreDifferent() {
        SendEmailWorker worker = new SendEmailWorker();
        Task task = taskWith(new HashMap<>());
        task.setWorkflowInstanceId("wf-diff");
        TaskResult result = worker.execute(task);

        String approveUrl = (String) result.getOutputData().get("approveUrl");
        String rejectUrl = (String) result.getOutputData().get("rejectUrl");
        assertNotEquals(approveUrl, rejectUrl);
    }

    @Test
    void deterministicOutputForSameInput() {
        SendEmailWorker worker = new SendEmailWorker();

        Task task1 = taskWith(new HashMap<>());
        task1.setWorkflowInstanceId("wf-det");
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(new HashMap<>());
        task2.setWorkflowInstanceId("wf-det");
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("approveUrl"), result2.getOutputData().get("approveUrl"));
        assertEquals(result1.getOutputData().get("rejectUrl"), result2.getOutputData().get("rejectUrl"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
