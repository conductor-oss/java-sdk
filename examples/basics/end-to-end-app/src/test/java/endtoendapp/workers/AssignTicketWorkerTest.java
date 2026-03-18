package endtoendapp.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AssignTicketWorkerTest {

    private final AssignTicketWorker worker = new AssignTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("assign_ticket", worker.getTaskDefName());
    }

    @Test
    void assignsBillingToFinanceSupport() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-200",
                "category", "billing",
                "priority", "LOW"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Finance Support", result.getOutputData().get("team"));
        assertEquals("4 hours", result.getOutputData().get("estimatedResponse"));
    }

    @Test
    void upgradesResponseTimeForCritical() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-201",
                "category", "technical",
                "priority", "CRITICAL"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Engineering", result.getOutputData().get("team"));
        assertEquals("30 minutes", result.getOutputData().get("estimatedResponse"));
    }

    @Test
    void defaultsUnknownCategoryToGeneralSupport() {
        Task task = taskWith(Map.of(
                "ticketId", "TKT-202",
                "category", "unknown_category",
                "priority", "MEDIUM"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("General Support", result.getOutputData().get("team"));
        assertEquals("24 hours", result.getOutputData().get("estimatedResponse"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
