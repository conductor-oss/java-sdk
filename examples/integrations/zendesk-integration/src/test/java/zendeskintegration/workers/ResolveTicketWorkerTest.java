package zendeskintegration.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ResolveTicketWorkerTest {

    private final ResolveTicketWorker worker = new ResolveTicketWorker();

    @Test
    void taskDefName() {
        assertEquals("zd_resolve", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("ticketId", "ZD-12345", "agentId", "agent-007"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("resolved"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
