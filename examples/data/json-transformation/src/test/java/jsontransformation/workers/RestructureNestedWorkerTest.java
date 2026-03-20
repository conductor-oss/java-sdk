package jsontransformation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RestructureNestedWorkerTest {

    private final RestructureNestedWorker worker = new RestructureNestedWorker();

    @Test
    void taskDefName() {
        assertEquals("jt_restructure_nested", worker.getTaskDefName());
    }

    @Test
    void restructuresIntoIdentityContactAccount() {
        Task task = taskWith(Map.of(
                "mapped", Map.of(
                        "customerId", "C-9001",
                        "fullName", "Jane Doe",
                        "emailAddress", "jane.doe@example.com",
                        "phoneNumber", "+1-555-0199",
                        "accountType", "PREMIUM",
                        "registrationDate", "2024-01-15")));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        assertNotNull(restructured.get("identity"));
        assertNotNull(restructured.get("contact"));
        assertNotNull(restructured.get("account"));
    }

    @Test
    void identityContainsIdAndName() {
        Task task = taskWith(Map.of(
                "mapped", Map.of("customerId", "C-100", "fullName", "Bob Smith")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        @SuppressWarnings("unchecked")
        Map<String, Object> identity = (Map<String, Object>) restructured.get("identity");
        assertEquals("C-100", identity.get("id"));
        assertEquals("Bob Smith", identity.get("name"));
    }

    @Test
    void contactContainsEmailAndPhone() {
        Task task = taskWith(Map.of(
                "mapped", Map.of("emailAddress", "alice@example.com", "phoneNumber", "555-9999")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        @SuppressWarnings("unchecked")
        Map<String, Object> contact = (Map<String, Object>) restructured.get("contact");
        assertEquals("alice@example.com", contact.get("email"));
        assertEquals("555-9999", contact.get("phone"));
    }

    @Test
    void accountContainsTypeAndRegisteredAt() {
        Task task = taskWith(Map.of(
                "mapped", Map.of("accountType", "PREMIUM", "registrationDate", "2024-01-15")));
        TaskResult result = worker.execute(task);

        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        @SuppressWarnings("unchecked")
        Map<String, Object> account = (Map<String, Object>) restructured.get("account");
        assertEquals("PREMIUM", account.get("type"));
        assertEquals("2024-01-15", account.get("registeredAt"));
    }

    @Test
    void handlesNullMapped() {
        Map<String, Object> input = new HashMap<>();
        input.put("mapped", null);
        Task task = taskWith(input);
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        assertNotNull(restructured.get("identity"));
        assertNotNull(restructured.get("contact"));
        assertNotNull(restructured.get("account"));
    }

    @Test
    void handlesMissingMappedKey() {
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertNotNull(result.getOutputData().get("restructured"));
    }

    @Test
    void handlesEmptyMapped() {
        Task task = taskWith(Map.of("mapped", Map.of()));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        @SuppressWarnings("unchecked")
        Map<String, Object> restructured = (Map<String, Object>) result.getOutputData().get("restructured");
        @SuppressWarnings("unchecked")
        Map<String, Object> identity = (Map<String, Object>) restructured.get("identity");
        assertNull(identity.get("id"));
        assertNull(identity.get("name"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
