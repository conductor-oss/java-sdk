package retrospective.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class PrioritizeWorker implements Worker {
    @Override public String getTaskDefName() { return "rsp_prioritize"; }

    @Override
    public TaskResult execute(Task task) {
        String sprintId = (String) task.getInputData().get("sprintId");
        System.out.println("  [Prioritize] Prioritizing items for sprint " + sprintId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("priorities", List.of(
            Map.of("item", "CI/CD optimization", "priority", "high", "votes", 4),
            Map.of("item", "Testing standards", "priority", "high", "votes", 3),
            Map.of("item", "Documentation updates", "priority", "medium", "votes", 2)
        ));
        return result;
    }
}
