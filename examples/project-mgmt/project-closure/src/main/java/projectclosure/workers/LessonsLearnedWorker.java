package projectclosure.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class LessonsLearnedWorker implements Worker {
    @Override public String getTaskDefName() { return "pcl_lessons_learned"; }

    @Override
    public TaskResult execute(Task task) {
        String projectId = (String) task.getInputData().get("projectId");
        System.out.println("  [Lessons Learned] Documenting lessons for project " + projectId);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("report", Map.of(
            "projectId", projectId,
            "lessons", List.of(
                "Early stakeholder engagement improved requirements quality",
                "Automated testing reduced defect rate by 40%",
                "Weekly demos kept team aligned with goals"
            ),
            "recommendations", List.of(
                "Adopt CI/CD earlier in project lifecycle",
                "Increase cross-team collaboration sessions"
            )
        ));
        return result;
    }
}
