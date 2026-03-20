package scholarshipprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AwardWorker implements Worker {
    @Override public String getTaskDefName() { return "scp_award"; }
    @Override public TaskResult execute(Task task) {
        int rank = task.getInputData().get("rank") instanceof Number ? ((Number) task.getInputData().get("rank")).intValue() : 99;
        boolean awarded = rank <= 2;
        int amount = rank == 1 ? 10000 : rank == 2 ? 5000 : 0;
        System.out.println("  [award] " + task.getInputData().get("applicationId") + ": " + (awarded ? "awarded $" + amount : "not awarded"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("awarded", awarded);
        result.getOutputData().put("amount", amount);
        return result;
    }
}
