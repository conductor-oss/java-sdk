package retrospective.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class CollectFeedbackWorker implements Worker {
    @Override public String getTaskDefName() { return "rsp_collect_feedback"; }

    @Override
    public TaskResult execute(Task task) {
        String sprintId = (String) task.getInputData().get("sprintId");
        System.out.println("  [Collect Feedback] Gathering feedback for sprint " + sprintId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("feedback", List.of(
            Map.of("author", "Alice", "type", "positive", "comment", "Great collaboration on the API redesign"),
            Map.of("author", "Bob", "type", "improvement", "comment", "Need better test coverage for edge cases"),
            Map.of("author", "Carol", "type", "positive", "comment", "Daily standups were effective"),
            Map.of("author", "Dave", "type", "improvement", "comment", "Deployment pipeline needs optimization")
        ));
        result.getOutputData().put("responseCount", 4);
        return result;
    }
}
