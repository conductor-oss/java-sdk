package couponengine.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.*;

public class CheckEligibilityWorker implements Worker {
    @Override public String getTaskDefName() { return "cpn_check_eligibility"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        double cartTotal = toDouble(task.getInputData().get("cartTotal"));
        Map<String, Object> rules = (Map<String, Object>) task.getInputData().getOrDefault("couponRules", Map.of());
        double minCartTotal = toDouble(rules.getOrDefault("minCartTotal", 0));
        boolean meetsMinimum = cartTotal >= minCartTotal;
        boolean notExpired = true;
        int maxUses = toInt(rules.getOrDefault("maxUses", 100));
        int currentUses = toInt(rules.getOrDefault("currentUses", 0));
        boolean usesRemaining = (maxUses - currentUses) > 0;
        boolean eligible = meetsMinimum && notExpired && usesRemaining;

        System.out.println("  [eligible] Customer " + task.getInputData().get("customerId")
                + ": cart=$" + cartTotal + ", min=$" + minCartTotal + ", eligible=" + eligible);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("eligible", eligible);
        output.put("meetsMinimum", meetsMinimum);
        output.put("notExpired", notExpired);
        output.put("usesRemaining", usesRemaining);
        result.setOutputData(output);
        return result;
    }

    private double toDouble(Object o) { if (o instanceof Number) return ((Number)o).doubleValue(); try { return Double.parseDouble(o.toString()); } catch (Exception e) { return 0; } }
    private int toInt(Object o) { if (o instanceof Number) return ((Number)o).intValue(); try { return Integer.parseInt(o.toString()); } catch (Exception e) { return 0; } }
}
