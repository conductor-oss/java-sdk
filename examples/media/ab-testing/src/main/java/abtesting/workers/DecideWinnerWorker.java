package abtesting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DecideWinnerWorker implements Worker {
    @Override public String getTaskDefName() { return "abt_decide_winner"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [decide] Processing " + task.getInputData().getOrDefault("recommendation", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        String winner = (String) task.getInputData().getOrDefault("winner", "inconclusive");
        String rec = !"inconclusive".equals(winner) ? "Roll out variant " + winner : "No clear winner — extend test";
        r.getOutputData().put("recommendation", rec);
        return r;
    }
}
