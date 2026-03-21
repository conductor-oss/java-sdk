package signals.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SigCompleteWorkerTest {

    private final SigCompleteWorker worker = new SigCompleteWorker();

    @Test
    void taskDefName() {
        assertEquals("sig_complete", worker.getTaskDefName());
    }

    @Test
    void completesOrderSuccessfully() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "deliveredAt", "2026-03-14T10:30:00Z",
                "signature", "J. Smith"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals("2026-03-14T10:30:00Z", result.getOutputData().get("deliveredAt"));
        assertEquals("J. Smith", result.getOutputData().get("signature"));
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void returnsDoneTrue() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "deliveredAt", "2026-03-14T11:00:00Z",
                "signature", "A. Jones"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void passesAllFieldsThrough() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "deliveredAt", "2026-03-15T09:00:00Z",
                "signature", "B. Wilson"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("ORD-300", result.getOutputData().get("orderId"));
        assertEquals("2026-03-15T09:00:00Z", result.getOutputData().get("deliveredAt"));
        assertEquals("B. Wilson", result.getOutputData().get("signature"));
    }

    @Test
    void defaultsFieldsWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("N/A", result.getOutputData().get("deliveredAt"));
        assertEquals("N/A", result.getOutputData().get("signature"));
        assertEquals(true, result.getOutputData().get("done"));
    }

    @Test
    void defaultsFieldsWhenBlank() {
        Task task = taskWith(Map.of(
                "orderId", "   ",
                "deliveredAt", "  ",
                "signature", " "
        ));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("N/A", result.getOutputData().get("deliveredAt"));
        assertEquals("N/A", result.getOutputData().get("signature"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
