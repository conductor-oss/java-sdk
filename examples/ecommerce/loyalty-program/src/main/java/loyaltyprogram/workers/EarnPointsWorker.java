package loyaltyprogram.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.*;

public class EarnPointsWorker implements Worker {
    @Override public String getTaskDefName() { return "loy_earn_points"; }
    @Override public TaskResult execute(Task task) {
        double amount = 0; Object a = task.getInputData().get("purchaseAmount"); if (a instanceof Number) amount = ((Number)a).doubleValue();
        int multiplier = "Gold".equals(task.getInputData().get("currentTier")) ? 2 : 1;
        int pointsEarned = (int) Math.round(amount * multiplier);
        int totalPoints = 4500 + pointsEarned;
        System.out.println("  [earn] +" + pointsEarned + " points (" + multiplier + "x multiplier), total: " + totalPoints);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> o = new LinkedHashMap<>(); o.put("pointsEarned", pointsEarned); o.put("totalPoints", totalPoints); o.put("multiplier", multiplier);
        r.setOutputData(o); return r;
    }
}
