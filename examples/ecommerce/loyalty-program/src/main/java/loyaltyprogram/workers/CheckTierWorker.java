package loyaltyprogram.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class CheckTierWorker implements Worker {
    @Override public String getTaskDefName() { return "loy_check_tier"; }
    @Override public TaskResult execute(Task task) {
        int total = 0; Object t = task.getInputData().get("totalPoints"); if (t instanceof Number) total = ((Number)t).intValue();
        String current = (String) task.getInputData().getOrDefault("currentTier", "Bronze");
        String newTier = current; boolean upgradeEligible = false;
        if (total >= 5000 && "Silver".equals(current)) { newTier = "Gold"; upgradeEligible = true; }
        else if (total >= 2000 && "Bronze".equals(current)) { newTier = "Silver"; upgradeEligible = true; }
        System.out.println("  [tier] Points: " + total + ", Current: " + current + ", Eligible: " + (upgradeEligible ? newTier : "no upgrade"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("newTier", newTier); o.put("upgradeEligible", String.valueOf(upgradeEligible)); o.put("pointsToNext", "Gold".equals(newTier) ? 0 : 5000 - total);
        r.setOutputData(o); return r;
    }
}
