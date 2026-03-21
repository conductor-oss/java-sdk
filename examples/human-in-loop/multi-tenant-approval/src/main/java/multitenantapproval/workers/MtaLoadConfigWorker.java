package multitenantapproval.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Worker for mta_load_config task -- loads tenant configuration and determines
 * the required approval level based on tenantId and amount.
 *
 * Tenant configs (hardcoded):
 * - "startup-co": autoApproveLimit=5000, levels=["manager"]
 * - "enterprise-corp": autoApproveLimit=1000, levels=["manager","executive"]
 * - "small-biz": autoApproveLimit=10000, levels=[]
 *
 * If amount > autoApproveLimit:
 *   - executive if 2+ levels
 *   - manager if 1 level
 *   - none if 0 levels
 * Else: none
 */
public class MtaLoadConfigWorker implements Worker {

    private static final Map<String, TenantConfig> TENANT_CONFIGS = Map.of(
            "startup-co", new TenantConfig(5000, List.of("manager")),
            "enterprise-corp", new TenantConfig(1000, List.of("manager", "executive")),
            "small-biz", new TenantConfig(10000, List.of())
    );

    @Override
    public String getTaskDefName() {
        return "mta_load_config";
    }

    @Override
    public TaskResult execute(Task task) {
        String tenantId = (String) task.getInputData().get("tenantId");
        double amount = toDouble(task.getInputData().get("amount"));

        System.out.println("  [mta_load_config] Loading config for tenant=" + tenantId + ", amount=" + amount);

        TenantConfig config = TENANT_CONFIGS.get(tenantId);

        String approvalLevel;
        if (config == null) {
            approvalLevel = "none";
        } else if (amount > config.autoApproveLimit()) {
            int levelCount = config.levels().size();
            if (levelCount >= 2) {
                approvalLevel = "executive";
            } else if (levelCount == 1) {
                approvalLevel = "manager";
            } else {
                approvalLevel = "none";
            }
        } else {
            approvalLevel = "none";
        }

        System.out.println("  [mta_load_config] Approval level: " + approvalLevel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("approvalLevel", approvalLevel);
        result.getOutputData().put("tenantId", tenantId);
        result.getOutputData().put("amount", amount);

        return result;
    }

    private double toDouble(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    record TenantConfig(int autoApproveLimit, List<String> levels) {}
}
