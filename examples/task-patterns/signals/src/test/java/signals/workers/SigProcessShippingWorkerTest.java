package signals.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SigProcessShippingWorkerTest {

    private final SigProcessShippingWorker worker = new SigProcessShippingWorker();

    @Test
    void taskDefName() {
        assertEquals("sig_process_shipping", worker.getTaskDefName());
    }

    @Test
    void processesShippingSuccessfully() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-100",
                "trackingNumber", "TRK-555",
                "carrier", "UPS"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ORD-100", result.getOutputData().get("orderId"));
        assertEquals("TRK-555", result.getOutputData().get("trackingNumber"));
        assertEquals("UPS", result.getOutputData().get("carrier"));
        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void returnsNotifiedTrue() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-200",
                "trackingNumber", "TRK-814",
                "carrier", "FedEx"
        ));
        TaskResult result = worker.execute(task);

        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void passesAllFieldsThrough() {
        Task task = taskWith(Map.of(
                "orderId", "ORD-300",
                "trackingNumber", "TRK-888",
                "carrier", "DHL"
        ));
        TaskResult result = worker.execute(task);

        assertEquals("ORD-300", result.getOutputData().get("orderId"));
        assertEquals("TRK-888", result.getOutputData().get("trackingNumber"));
        assertEquals("DHL", result.getOutputData().get("carrier"));
    }

    @Test
    void defaultsFieldsWhenMissing() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("N/A", result.getOutputData().get("trackingNumber"));
        assertEquals("N/A", result.getOutputData().get("carrier"));
        assertEquals(true, result.getOutputData().get("notified"));
    }

    @Test
    void defaultsFieldsWhenBlank() {
        Task task = taskWith(Map.of(
                "orderId", "   ",
                "trackingNumber", "  ",
                "carrier", " "
        ));
        TaskResult result = worker.execute(task);

        assertEquals("unknown", result.getOutputData().get("orderId"));
        assertEquals("N/A", result.getOutputData().get("trackingNumber"));
        assertEquals("N/A", result.getOutputData().get("carrier"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
