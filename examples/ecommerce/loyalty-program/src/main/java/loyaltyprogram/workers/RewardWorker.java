package loyaltyprogram.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.time.Instant;
import java.util.*;

public class RewardWorker implements Worker {
    private static final Map<String, String> REWARDS = Map.of(
            "Bronze", "5% off next purchase", "Silver", "10% off + free shipping", "Gold", "15% off + free shipping + early access");
    @Override public String getTaskDefName() { return "loy_reward"; }
    @Override public TaskResult execute(Task task) {
        String tier = (String) task.getInputData().getOrDefault("tier", "Bronze");
        String reward = REWARDS.getOrDefault(tier, REWARDS.get("Bronze"));
        System.out.println("  [reward] " + tier + " reward: " + reward);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("reward", reward); o.put("tier", tier); o.put("deliveredAt", Instant.now().toString());
        r.setOutputData(o); return r;
    }
}
