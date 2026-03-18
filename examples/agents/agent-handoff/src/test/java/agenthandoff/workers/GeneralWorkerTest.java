package agenthandoff.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class GeneralWorkerTest {

    private final GeneralWorker worker = new GeneralWorker();

    @Test
    void taskDefName() {
        assertEquals("ah_general", worker.getTaskDefName());
    }

    // --- Resolution routing by message content ---

    @Test
    void planInquiryResolution() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-300",
                "message", "Tell me about your plans",
                "triageNotes", "General inquiry",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        String resolution = (String) result.getOutputData().get("resolution");
        assertNotNull(resolution);
        assertTrue(resolution.contains("plan"));
        assertTrue(resolution.contains("CUST-300"));
    }

    @Test
    void cancellationInquiryResolution() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-301",
                "message", "I want to cancel my account",
                "triageNotes", "General inquiry",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        String resolution = (String) result.getOutputData().get("resolution");
        assertTrue(resolution.contains("cancellation"));
    }

    @Test
    void genericInquiryResolution() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-302",
                "message", "I need help with something",
                "triageNotes", "General inquiry",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        String resolution = (String) result.getOutputData().get("resolution");
        assertTrue(resolution.contains("CUST-302"));
        assertTrue(resolution.contains("Follow-up") || resolution.contains("follow-up"));
    }

    // --- Actions ---

    @Test
    void planInquiryActions() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-310",
                "message", "What pricing plans do you have?",
                "triageNotes", "General",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) result.getOutputData().get("actions");
        assertNotNull(actions);
        assertTrue(actions.contains("plan_info_sent"));
        assertTrue(actions.contains("upgrade_recommendation"));
    }

    @Test
    void cancellationActions() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-311",
                "message", "I want to cancel",
                "triageNotes", "General",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        List<String> actions = (List<String>) result.getOutputData().get("actions");
        assertTrue(actions.contains("retention_callback_scheduled"));
    }

    // --- Ticket ID ---

    @Test
    void ticketIdIsDeterministicPerCustomer() {
        Task task1 = taskWith(Map.of("customerId", "CUST-320", "message", "hi", "triageNotes", "", "urgency", "normal"));
        Task task2 = taskWith(Map.of("customerId", "CUST-320", "message", "hello", "triageNotes", "", "urgency", "normal"));

        String ticket1 = (String) worker.execute(task1).getOutputData().get("ticketId");
        String ticket2 = (String) worker.execute(task2).getOutputData().get("ticketId");

        assertTrue(ticket1.startsWith("GEN-"));
        assertEquals(ticket1, ticket2);
    }

    // --- Output shape ---

    @Test
    void outputContainsAllExpectedFields() {
        Task task = taskWith(Map.of(
                "customerId", "CUST-303",
                "message", "Hello there",
                "triageNotes", "notes",
                "urgency", "normal"));
        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("specialist"));
        assertTrue(result.getOutputData().containsKey("ticketId"));
        assertTrue(result.getOutputData().containsKey("resolution"));
        assertTrue(result.getOutputData().containsKey("actions"));
        assertEquals(4, result.getOutputData().size());
        assertEquals("general", result.getOutputData().get("specialist"));
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
