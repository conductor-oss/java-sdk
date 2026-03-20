package zendeskintegration.workers;

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
        assertEquals("zd_classify", worker.getTaskDefName());
    }

    @Test
    void executes() {
        Task task = taskWith(Map.of("ticketId", "ZD-12345", "subject", "Cannot access dashboard", "description", "Error 403"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("normal", result.getOutputData().get("priority"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
