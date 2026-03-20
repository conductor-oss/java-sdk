package playerprogression.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class CheckLevelWorker implements Worker {
    @Override public String getTaskDefName() { return "ppg_check_level"; }
    @Override public TaskResult execute(Task task) {
        int totalXp = 5300;
        Object tx = task.getInputData().get("totalXp");
        if (tx instanceof Number) totalXp = ((Number) tx).intValue();
        int newLevel = totalXp / 1000;
        boolean leveledUp = newLevel > 4;
        System.out.println("  [level] Total XP: " + totalXp + ", Level: " + newLevel + ", Leveled up: " + leveledUp);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("newLevel", newLevel); r.addOutputData("leveledUp", leveledUp); r.addOutputData("totalXp", totalXp);
        return r;
    }
}
