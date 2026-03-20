package subscriptionbilling.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class GenerateInvoiceWorkerTest {
    private final GenerateInvoiceWorker worker = new GenerateInvoiceWorker();

    @Test void taskDefName() { assertEquals("sub_generate_invoice", worker.getTaskDefName()); }

    @Test void professionalPlanPrice() {
        Task task = taskWith(Map.of("subscriptionId", "sub-1", "customerId", "c1", "plan", "professional", "periodStart", "2024-01-01", "periodEnd", "2024-02-01"));
        TaskResult r = worker.execute(task);
        assertEquals(29.99, ((Number) r.getOutputData().get("amount")).doubleValue());
        assertTrue(r.getOutputData().get("invoiceId").toString().startsWith("INV-"));
    }

    @Test void starterPlanPrice() {
        Task task = taskWith(Map.of("subscriptionId", "sub-1", "customerId", "c1", "plan", "starter", "periodStart", "2024-01-01", "periodEnd", "2024-02-01"));
        TaskResult r = worker.execute(task);
        assertEquals(9.99, ((Number) r.getOutputData().get("amount")).doubleValue());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task(); task.setStatus(Task.Status.IN_PROGRESS); task.setInputData(new HashMap<>(input)); return task;
    }
}
