package invoiceprocessing.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class OcrExtractWorkerTest {
    private final OcrExtractWorker worker = new OcrExtractWorker();

    @Test void taskDefName() { assertEquals("ivc_ocr_extract", worker.getTaskDefName()); }

    @Test void extractsData() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "INV-1", "documentUrl", "http://test.pdf")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertTrue(((Number) r.getOutputData().get("amount")).doubleValue() > 0);
        assertNotNull(r.getOutputData().get("poNumber"));
    }

    @Test void computesTaxCorrectly() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "INV-2")));
        TaskResult r = worker.execute(t);
        double subtotal = ((Number) r.getOutputData().get("subtotal")).doubleValue();
        double tax = ((Number) r.getOutputData().get("tax")).doubleValue();
        double total = ((Number) r.getOutputData().get("amount")).doubleValue();
        assertEquals(total, subtotal + tax, 0.01);
    }

    @Test void lineItemTotalsMatchSubtotal() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "INV-3")));
        TaskResult r = worker.execute(t);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) r.getOutputData().get("lineItems");
        double lineSum = items.stream().mapToDouble(i -> ((Number) i.get("total")).doubleValue()).sum();
        assertEquals(((Number) r.getOutputData().get("subtotal")).doubleValue(), lineSum, 0.01);
    }

    @Test void lineItemArithmeticIsValid() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "INV-ARITH-CHECK")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) r.getOutputData().get("lineItems");
        for (Map<String, Object> item : items) {
            double unitPrice = ((Number) item.get("unitPrice")).doubleValue();
            int qty = ((Number) item.get("quantity")).intValue();
            double lineTotal = ((Number) item.get("total")).doubleValue();
            assertEquals(unitPrice * qty, lineTotal, 0.01,
                    "Line item total must equal unitPrice * quantity");
        }
    }

    // ---- Failure path ---------------------------------------------------

    @Test void failsWithTerminalErrorOnMissingInvoiceId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>());
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("invoiceId"));
    }

    @Test void failsWithTerminalErrorOnInvalidInvoiceIdFormat() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "MALFORMED-123")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
        assertTrue(r.getReasonForIncompletion().contains("Invalid invoiceId format"));
    }

    @Test void failsWithTerminalErrorOnBlankInvoiceId() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("invoiceId", "  ")));
        TaskResult r = worker.execute(t);
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, r.getStatus());
    }
}
