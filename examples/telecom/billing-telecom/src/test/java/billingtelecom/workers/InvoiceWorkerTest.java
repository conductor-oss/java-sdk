package billingtelecom.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InvoiceWorkerTest {

    @Test
    void testInvoiceWorker() {
        InvoiceWorker worker = new InvoiceWorker();
        assertEquals("btl_invoice", worker.getTaskDefName());
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(Map.of("customerId", "CUST-811", "ratedCharges", "charges"));
        TaskResult result = worker.execute(task);
        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("INV-billing-telecom-001", result.getOutputData().get("invoiceId"));
        assertEquals(54.5, result.getOutputData().get("totalAmount"));
    }
}
