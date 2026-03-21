package assessmentcreation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class DefineCriteriaWorker implements Worker {
    @Override public String getTaskDefName() { return "asc_define_criteria"; }

    @Override
    public TaskResult execute(Task task) {
        String assessmentType = (String) task.getInputData().get("assessmentType");
        String courseId = (String) task.getInputData().get("courseId");
        List<String> questionTypes = List.of("multiple_choice", "short_answer", "coding");
        Map<String, Object> criteria = Map.of(
                "difficulty", "mixed",
                "questionTypes", questionTypes,
                "topicCoverage", task.getInputData().getOrDefault("topics", List.of()),
                "totalPoints", 100);
        System.out.println("  [criteria] " + assessmentType + " for " + courseId + ": " + questionTypes.size() + " question types");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("criteria", criteria);
        result.getOutputData().put("criteriaCount", questionTypes.size());
        return result;
    }
}
