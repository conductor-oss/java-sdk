package zendeskintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CreateTicketWorkerTest {

    private final CreateTicketWorker worker = new CreateTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("zd_create_ticket", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("requesterEmail", "user@test.com", "subject", "Cannot access dashboard", "description", "Error 403", "category", "access"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("ticketId"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
