package multistepcompensation.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WorkerTests {

    // --- CreateAccountWorker tests ---

    @Test
    void createAccountTaskDefName() {
        CreateAccountWorker worker = new CreateAccountWorker();
        assertEquals("msc_create_account", worker.getTaskDefName());
    }

    @Test
    void createAccountReturnsAccountId() {
        CreateAccountWorker worker = new CreateAccountWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("ACCT-001", result.getOutputData().get("accountId"));
    }

    // --- SetupBillingWorker tests ---

    @Test
    void setupBillingTaskDefName() {
        SetupBillingWorker worker = new SetupBillingWorker();
        assertEquals("msc_setup_billing", worker.getTaskDefName());
    }

    @Test
    void setupBillingReturnsBillingId() {
        SetupBillingWorker worker = new SetupBillingWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("BILL-001", result.getOutputData().get("billingId"));
    }

    // --- ProvisionResourcesWorker tests ---

    @Test
    void provisionResourcesTaskDefName() {
        ProvisionResourcesWorker worker = new ProvisionResourcesWorker();
        assertEquals("msc_provision_resources", worker.getTaskDefName());
    }

    @Test
    void provisionResourcesFailsWhenFailAtProvision() {
        ProvisionResourcesWorker worker = new ProvisionResourcesWorker();
        Task task = taskWith(Map.of("failAt", "provision"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.FAILED, result.getStatus());
        assertNotNull(result.getOutputData().get("error"));
    }

    @Test
    void provisionResourcesSucceedsWhenFailAtNone() {
        ProvisionResourcesWorker worker = new ProvisionResourcesWorker();
        Task task = taskWith(Map.of("failAt", "none"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RES-001", result.getOutputData().get("resourceId"));
    }

    @Test
    void provisionResourcesSucceedsWhenNoFailAt() {
        ProvisionResourcesWorker worker = new ProvisionResourcesWorker();
        Task task = taskWith(Map.of());
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("RES-001", result.getOutputData().get("resourceId"));
    }

    // --- UndoProvisionWorker tests ---

    @Test
    void undoProvisionTaskDefName() {
        UndoProvisionWorker worker = new UndoProvisionWorker();
        assertEquals("msc_undo_provision", worker.getTaskDefName());
    }

    @Test
    void undoProvisionReturnsUndone() {
        UndoProvisionWorker worker = new UndoProvisionWorker();
        Task task = taskWith(Map.of("resourceId", "RES-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    // --- UndoBillingWorker tests ---

    @Test
    void undoBillingTaskDefName() {
        UndoBillingWorker worker = new UndoBillingWorker();
        assertEquals("msc_undo_billing", worker.getTaskDefName());
    }

    @Test
    void undoBillingReturnsUndone() {
        UndoBillingWorker worker = new UndoBillingWorker();
        Task task = taskWith(Map.of("billingId", "BILL-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    // --- UndoAccountWorker tests ---

    @Test
    void undoAccountTaskDefName() {
        UndoAccountWorker worker = new UndoAccountWorker();
        assertEquals("msc_undo_account", worker.getTaskDefName());
    }

    @Test
    void undoAccountReturnsUndone() {
        UndoAccountWorker worker = new UndoAccountWorker();
        Task task = taskWith(Map.of("accountId", "ACCT-001"));
        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals(true, result.getOutputData().get("undone"));
    }

    // --- Helper ---

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
