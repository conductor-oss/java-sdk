package zendeskintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RouteTicketWorkerTest {

    private final RouteTicketWorker worker = new RouteTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("zd_route", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("ticketId", "ZD-12345", "priority", "normal", "category", "access"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("Mike Chen", result.getOutputData().get("agentName"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
