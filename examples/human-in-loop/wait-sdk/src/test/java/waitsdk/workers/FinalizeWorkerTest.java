package waitsdk.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FinalizeWorkerTest {

    @Test
    void taskDefName() {
        FinalizeWorker worker = new FinalizeWorker();
        assertEquals("wsdk_finalize", worker.getTaskDefName());
    }

    @Test
    void returnsClosedResultWithTicketId() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("ticketId", "TKT-100"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("closed-TKT-100", result.getOutputData().get("result"));
    }

    @Test
    void returnsClosedResultWithEmptyTicketId() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("closed-", result.getOutputData().get("result"));
    }

    @Test
    void outputContainsResultKey() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("ticketId", "XYZ"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("result"));
    }

    @Test
    void handlesNumericTicketId() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task = taskWith(Map.of("ticketId", 99));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("closed-99", result.getOutputData().get("result"));
    }

    @Test
    void deterministic_same_input_same_output() {
        FinalizeWorker worker = new FinalizeWorker();
        Task task1 = taskWith(Map.of("ticketId", "DET-1"));
        Task task2 = taskWith(Map.of("ticketId", "DET-1"));

        TaskResult result1 = worker.execute(task1);
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("result"), result2.getOutputData().get("result"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
