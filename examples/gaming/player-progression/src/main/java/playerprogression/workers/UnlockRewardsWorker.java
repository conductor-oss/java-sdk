package playerprogression.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
public class UnlockRewardsWorker implements Worker {
    @Override public String getTaskDefName() { return "ppg_unlock_rewards"; }
    @Override public TaskResult execute(Task task) {
        Object lu = task.getInputData().get("leveledUp");
        boolean leveledUp = Boolean.TRUE.equals(lu) || "true".equals(String.valueOf(lu));
        System.out.println("  [rewards] " + (leveledUp ? "Unlocking level-up rewards!" : "No new rewards"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("rewards", leveledUp ? List.of("Gold Shield", "Fire Spell", "Title: Dragon Slayer") : List.of());
        return r;
    }
}
