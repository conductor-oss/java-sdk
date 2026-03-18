package agenthandoff.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class BillingWorkerTest {

    private final BillingWorker worker = new BillingWorker();

    @Test
    void taskDefName() {
        assertEquals("ah_billing", worker.getTaskDefName());
    }

    // --- Resolution routing by message content ---

    @Test
    void refundResolutionWhenMessageContainsRefund() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-100",
                "message", "I need a refund for the duplicate charge",
                "triageNotes", "Billing issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String resolution = (String) result.getOutputData().get("resolution");
        assertNotNull(resolution);
        assertTrue(resolution.contains("refund"));
        assertTrue(resolution.contains("CUST-100"));
    }

    @Test
    void invoiceResolutionWhenMessageContainsInvoice() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-101",
                "message", "Can you resend my invoice?",
                "triageNotes", "Billing issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        String resolution = (String) result.getOutputData().get("resolution");
        assertTrue(resolution.contains("invoice"));
    }

    @Test
    void generalBillingResolutionForOtherMessages() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-102",
                "message", "I have a question about my bill",
                "triageNotes", "Billing issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        String resolution = (String) result.getOutputData().get("resolution");
        assertTrue(resolution.contains("CUST-102"));
        assertTrue(resolution.contains("no discrepancies"));
    }

    // --- Actions ---

    @Test
    void refundActionsIncludeRefundAndEmail() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-110",
                "message", "I was overcharged last month",
                "triageNotes", "Billing issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) result.getOutputData().get("actions");
        assertNotNull(actions);
        assertTrue(actions.contains("refund_initiated"));
        assertTrue(actions.contains("email_confirmation_sent"));
    }

    @Test
    void highUrgencyAddsFlagForReview() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-111",
                "message", "I was overcharged",
                "triageNotes", "Billing issue",
                "urgency", "high"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) result.getOutputData().get("actions");
        assertTrue(actions.contains("case_flagged_for_review"));
    }

    @Test
    void normalUrgencyDoesNotFlagForReview() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-112",
                "message", "I was overcharged",
                "triageNotes", "Billing issue",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) result.getOutputData().get("actions");
        assertFalse(actions.contains("case_flagged_for_review"));
    }

    // --- Ticket ID ---

    @Test
    void ticketIdIsDeterministicPerCustomer() {
        Task task1 = taskWith(Map.of("customerId", "CUST-120", "message", "bill", "triageNotes", "", "urgency", "normal"));
        Task task2 = taskWith(Map.of("customerId", "CUST-120", "message", "charge", "triageNotes", "", "urgency", "normal"));

        String ticket1 = (String) worker.execute(task1).getOutputData().get("ticketId");
        String ticket2 = (String) worker.execute(task2).getOutputData().get("ticketId");

        assertTrue(ticket1.startsWith("BIL-"));
        assertEquals(ticket1, ticket2);
    }

    @Test
    void differentCustomersGetDifferentTickets() {
        Task task1 = taskWith(Map.of("customerId", "CUST-A", "message", "bill", "triageNotes", "", "urgency", "normal"));
        Task task2 = taskWith(Map.of("customerId", "CUST-B", "message", "bill", "triageNotes", "", "urgency", "normal"));

        String ticket1 = (String) worker.execute(task1).getOutputData().get("ticketId");
        String ticket2 = (String) worker.execute(task2).getOutputData().get("ticketId");

        assertNotEquals(ticket1, ticket2);
    }

    // --- Output shape ---

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-103",
                "message", "Invoice problem",
                "triageNotes", "notes",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("specialist"));
        assertTrue(result.getOutputData().containsKey("ticketId"));
        assertTrue(result.getOutputData().containsKey("resolution"));
        assertTrue(result.getOutputData().containsKey("actions"));
        assertEquals(4, result.getOutputData().size());
        assertEquals("billing", result.getOutputData().get("specialist"));
    }

    // --- Null / blank / missing inputs ---

    @Test
    void handlesNullInputs() {
        Map<String, Object> input = new HashMap<>();
        input.put("customerId", null);
        input.put("message", null);
        input.put("triageNotes", null);
        input.put("urgency", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("resolution"));
    }

    @Test
    void handlesMissingInputs() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("ticketId"));
    }

    @Test
    void handlesBlankInputs() {
        Task task = taskWith(Map.of(
                "customerId", "   ",
                "message", "   ",
                "triageNotes", "",
                "urgency", "   "));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
