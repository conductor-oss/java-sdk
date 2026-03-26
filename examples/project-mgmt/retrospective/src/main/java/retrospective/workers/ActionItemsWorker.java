package retrospective.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class ActionItemsWorker implements Worker {
    @Override public String getTaskDefName() { return "rsp_action_items"; }

    @Override
    public TaskResult execute(Task task) {
        String sprintId = (String) task.getInputData().get("sprintId");
        System.out.println("  [Action Items] Creating action items for sprint " + sprintId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("actionItems", List.of(
            Map.of("action", "Set up parallel test execution in CI", "owner", "Dave", "dueDate", "2026-03-28"),
            Map.of("action", "Define minimum test coverage thresholds", "owner", "Bob", "dueDate", "2026-03-21"),
            Map.of("action", "Create deployment runbook template", "owner", "Carol", "dueDate", "2026-04-04")
        ));
        result.getOutputData().put("totalItems", 3);
        return result;
    }
}
