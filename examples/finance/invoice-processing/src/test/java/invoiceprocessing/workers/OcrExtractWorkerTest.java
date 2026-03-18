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
}
