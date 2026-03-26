package personalization.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RankContentWorker implements Worker {
    @Override public String getTaskDefName() { return "per_rank_content"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rank] Processing " + task.getInputData().getOrDefault("rankedItems", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rankedItems", "ranked");
        r.getOutputData().put("rankingModel", "collaborative_filtering_v3");
        return r;
    }
}
