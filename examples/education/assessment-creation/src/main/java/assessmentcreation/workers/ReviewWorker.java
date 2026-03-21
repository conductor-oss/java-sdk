package assessmentcreation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class ReviewWorker implements Worker {
    @Override public String getTaskDefName() { return "asc_review"; }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<?> questions = (List<?>) task.getInputData().getOrDefault("questions", List.of());
        System.out.println("  [review] Reviewed " + questions.size() + " questions - all approved");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("status", "approved");
        result.getOutputData().put("reviewer", "Dr. Smith");
        return result;
    }
}
