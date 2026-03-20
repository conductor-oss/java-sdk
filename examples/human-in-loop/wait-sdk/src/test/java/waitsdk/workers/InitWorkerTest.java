package waitsdk.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class InitWorkerTest {

    @Test
    void taskDefName() {
        InitWorker worker = new InitWorker();
        assertEquals("wsdk_init", worker.getTaskDefName());
    }

    @Test
    void returnsOpenStatusWithTicketId() {
        InitWorker worker = new InitWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("TKT-100", result.getOutputData().get("ticketId"));
        assertEquals("open", result.getOutputData().get("status"));
    }

    @Test
    void returnsOpenStatusWithEmptyTicketId() {
        InitWorker worker = new InitWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("", result.getOutputData().get("ticketId"));
        assertEquals("open", result.getOutputData().get("status"));
    }

    @Test
    void outputAlwaysContainsStatusAndTicketId() {
        InitWorker worker = new InitWorker();
        Task task = taskWith(Map.of("ticketId", "ABC"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("ticketId"));
        assertTrue(result.getOutputData().containsKey("status"));
    }

    @Test
    void handlesNumericTicketId() {
        InitWorker worker = new InitWorker();
        Task task = taskWith(Map.of("ticketId", 42));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("42", result.getOutputData().get("ticketId"));
        assertEquals("open", result.getOutputData().get("status"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
