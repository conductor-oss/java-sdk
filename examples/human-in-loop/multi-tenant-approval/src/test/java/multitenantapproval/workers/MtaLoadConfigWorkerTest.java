package multitenantapproval.workers;

import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MtaLoadConfigWorkerTest {

    @Test
    void taskDefName() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        assertEquals("mta_load_config", worker.getTaskDefName());
    }

    // --- startup-co: autoApproveLimit=5000, levels=["manager"] ---

    @Test
    void startupCoUnderLimitReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "amount", 3000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    @Test
    void startupCoAtLimitReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "amount", 5000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    @Test
    void startupCoOverLimitReturnsManager() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "amount", 6000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("manager", result.getOutputData().get("approvalLevel"));
    }

    // --- enterprise-corp: autoApproveLimit=1000, levels=["manager","executive"] ---

    @Test
    void enterpriseCorpUnderLimitReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "enterprise-corp", "amount", 500));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    @Test
    void enterpriseCorpAtLimitReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "enterprise-corp", "amount", 1000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    @Test
    void enterpriseCorpOverLimitReturnsExecutive() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "enterprise-corp", "amount", 2000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("executive", result.getOutputData().get("approvalLevel"));
    }

    // --- small-biz: autoApproveLimit=10000, levels=[] ---

    @Test
    void smallBizUnderLimitReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "small-biz", "amount", 5000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    @Test
    void smallBizOverLimitReturnsNoneBecauseNoLevels() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "small-biz", "amount", 15000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    // --- unknown tenant ---

    @Test
    void unknownTenantReturnsNone() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "unknown-tenant", "amount", 100000));

        TaskResult result = worker.execute(task);

        assertEquals(TaskResult.Status.COMPLETED, result.getStatus());
        assertEquals("none", result.getOutputData().get("approvalLevel"));
    }

    // --- output contents ---

    @Test
    void outputContainsTenantIdAndAmount() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "amount", 3000));

        TaskResult result = worker.execute(task);

        assertEquals("startup-co", result.getOutputData().get("tenantId"));
        assertEquals(3000.0, result.getOutputData().get("amount"));
    }

    @Test
    void outputAlwaysContainsApprovalLevel() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();
        Task task = taskWith(Map.of("tenantId", "startup-co", "amount", 1000));

        TaskResult result = worker.execute(task);

        assertTrue(result.getOutputData().containsKey("approvalLevel"));
    }

    // --- deterministic: same input always gives same output ---

    @Test
    void deterministic() {
        MtaLoadConfigWorker worker = new MtaLoadConfigWorker();

        Task task1 = taskWith(Map.of("tenantId", "enterprise-corp", "amount", 5000));
        TaskResult result1 = worker.execute(task1);

        Task task2 = taskWith(Map.of("tenantId", "enterprise-corp", "amount", 5000));
        TaskResult result2 = worker.execute(task2);

        assertEquals(result1.getOutputData().get("approvalLevel"),
                result2.getOutputData().get("approvalLevel"));
    }

    private Task taskWith(Map<String, Object> input) {
        Task task = new Task();
        task.setStatus(Task.Status.IN_PROGRESS);
        task.setInputData(new HashMap<>(input));
        return task;
    }
}
