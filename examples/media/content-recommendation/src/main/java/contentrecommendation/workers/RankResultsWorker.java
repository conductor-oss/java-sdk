package contentrecommendation.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RankResultsWorker implements Worker {
    @Override public String getTaskDefName() { return "crm_rank_results"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [rank] Processing " + task.getInputData().getOrDefault("rankedItems", ""));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("rankedItems", "ranked");
        r.getOutputData().put("rankingStrategy", "hybrid_score");
        return r;
    }
}
