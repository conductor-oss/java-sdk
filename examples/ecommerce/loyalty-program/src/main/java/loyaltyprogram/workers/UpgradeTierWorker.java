package loyaltyprogram.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class UpgradeTierWorker implements Worker {
    @Override public String getTaskDefName() { return "loy_upgrade_tier"; }
    @Override public TaskResult execute(Task task) {
        boolean eligible = "true".equals(task.getInputData().get("eligible"));
        String activeTier = eligible ? (String) task.getInputData().get("newTier") : (String) task.getInputData().get("currentTier");
        if (eligible) System.out.println("  [upgrade] " + task.getInputData().get("currentTier") + " -> " + activeTier);
        else System.out.println("  [upgrade] No upgrade - staying at " + activeTier);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("activeTier", activeTier); o.put("upgraded", eligible);
        r.setOutputData(o); return r;
    }
}
