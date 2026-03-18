package endtoendapp.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NotifyCustomerWorkerTest {

    private final NotifyCustomerWorker worker = new NotifyCustomerWorker();

    @Test
    void taskDefName() {
        assertEquals("notify_customer", worker.getTaskDefName());
    }

    @Test
    void returnsSentTrue() {
        Task task = taskWith(Map.of(
                "customerEmail", "test@example.com",
                "ticketId", "TKT-300",
                "priority", "HIGH",
                "assignedTeam", "Engineering",
                "estimatedResponse", "1 hour"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("sent"));
        assertEquals("email", result.getOutputData().get("channel"));
    }

    @Test
    void outputHasChannelField() {
        Task task = taskWith(Map.of(
                "customerEmail", "test@example.com",
                "ticketId", "TKT-301",
                "priority", "LOW",
                "assignedTeam", "General Support",
                "estimatedResponse", "24 hours"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("email", result.getOutputData().get("channel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
