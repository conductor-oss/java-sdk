package scholarshipprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RankWorker implements Worker {
    @Override public String getTaskDefName() { return "scp_rank"; }
    @Override public TaskResult execute(Task task) {
        int score = task.getInputData().get("score") instanceof Number ? ((Number) task.getInputData().get("score")).intValue() : 0;
        int rank = score >= 90 ? 1 : score >= 80 ? 2 : 3;
        System.out.println("  [rank] " + task.getInputData().get("applicationId") + ": rank #" + rank);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("rank", rank);
        return result;
    }
}
