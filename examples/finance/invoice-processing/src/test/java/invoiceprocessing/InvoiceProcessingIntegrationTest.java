package invoiceprocessing;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import invoiceprocessing.workers.*;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test for the full invoice processing workflow data flow.
 * Tests: receive -> OCR extract -> match PO -> approve -> process payment.
 */
class InvoiceProcessingIntegrationTest {

    private final ReceiveInvoiceWorker receiveWorker = new ReceiveInvoiceWorker();
    private final OcrExtractWorker ocrWorker = new OcrExtractWorker();
    private final MatchPoWorker matchWorker = new MatchPoWorker();
    private final ApproveInvoiceWorker approveWorker = new ApproveInvoiceWorker();
    private final ProcessPaymentWorker payWorker = new ProcessPaymentWorker();

    @Test
    void fullPipeline_validInvoice() {
        // Step 1: Receive
        TaskResult receiveResult = exec(receiveWorker, Map.of(
                "invoiceId", "INV-2026-5500", "vendorId", "VND-330"));
        assertEquals(TaskResult.Status.COMPLETED, receiveResult.getStatus());
        assertEquals(true, receiveResult.getOutputData().get("validInvoice"));

        // Step 2: OCR Extract
        TaskResult ocrResult = exec(ocrWorker, Map.of("invoiceId", "INV-2026-5500"));
        assertEquals(TaskResult.Status.COMPLETED, ocrResult.getStatus());
        double amount = ((Number) ocrResult.getOutputData().get("amount")).doubleValue();
        assertTrue(amount > 0);
        String poNumber = (String) ocrResult.getOutputData().get("poNumber");
        assertNotNull(poNumber);

        // Verify arithmetic integrity
        double subtotal = ((Number) ocrResult.getOutputData().get("subtotal")).doubleValue();
        double tax = ((Number) ocrResult.getOutputData().get("tax")).doubleValue();
        assertEquals(amount, subtotal + tax, 0.01, "total must equal subtotal + tax");

        // Step 3: Match PO
        TaskResult matchResult = exec(matchWorker, Map.of(
                "extractedPoNumber", poNumber, "extractedAmount", amount));
        assertEquals(TaskResult.Status.COMPLETED, matchResult.getStatus());
        boolean matched = (boolean) matchResult.getOutputData().get("matched");

        // Step 4: Approve
        TaskResult approveResult = exec(approveWorker, Map.of(
                "amount", amount, "poMatched", matched));
        assertEquals(TaskResult.Status.COMPLETED, approveResult.getStatus());

        // Step 5: Process Payment (only if approved)
        boolean approved = (boolean) approveResult.getOutputData().get("approved");
        if (approved) {
            TaskResult payResult = exec(payWorker, Map.of(
                    "amount", amount, "vendorId", "VND-330"));
            assertEquals(TaskResult.Status.COMPLETED, payResult.getStatus());
            assertEquals("scheduled", payResult.getOutputData().get("paymentStatus"));
        }
    }

    @Test
    void malformedInvoiceId_failsAtReceive() {
        TaskResult result = exec(receiveWorker, Map.of(
                "invoiceId", "GARBAGE", "vendorId", "VND-330"));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Invalid invoiceId"));
    }

    @Test
    void malformedVendorId_failsAtReceive() {
        TaskResult result = exec(receiveWorker, Map.of(
                "invoiceId", "INV-123", "vendorId", "GARBAGE"));
        assertEquals(TaskResult.Status.FAILED_WITH_TERMINAL_ERROR, result.getStatus());
        assertTrue(result.getReasonForIncompletion().contains("Invalid vendorId"));
    }

    @Test
    void poMismatch_invoiceRejectedAtApproval() {
        // Force PO mismatch by using mismatched amounts
        TaskResult approveResult = exec(approveWorker, Map.of(
                "amount", 5000.0, "poMatched", false));
        assertEquals(TaskResult.Status.COMPLETED, approveResult.getStatus());
        assertEquals(false, approveResult.getOutputData().get("approved"));
        assertEquals("SYSTEM", approveResult.getOutputData().get("approver"));
    }

    private TaskResult exec(com.netflix.conductor.client.worker.Worker worker, Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return worker.execute(task);
    }
}
