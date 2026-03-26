package retrospective.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class CategorizeWorker implements Worker {
    @Override public String getTaskDefName() { return "rsp_categorize"; }

    @Override
    public TaskResult execute(Task task) {
        String sprintId = (String) task.getInputData().get("sprintId");
        System.out.println("  [Categorize] Categorizing feedback for sprint " + sprintId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("categories", Map.of(
            "wentWell", List.of("Team collaboration", "Effective standups"),
            "needsImprovement", List.of("Test coverage", "Deployment pipeline"),
            "actionNeeded", List.of("CI/CD optimization", "Testing standards")
        ));
        return result;
    }
}
