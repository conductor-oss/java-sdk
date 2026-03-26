package assessmentcreation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class CreateQuestionsWorker implements Worker {
    @Override public String getTaskDefName() { return "asc_create_questions"; }

    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> questions = List.of(
                Map.of("id", 1, "type", "multiple_choice", "points", 20, "topic", "sorting"),
                Map.of("id", 2, "type", "multiple_choice", "points", 20, "topic", "searching"),
                Map.of("id", 3, "type", "short_answer", "points", 20, "topic", "complexity"),
                Map.of("id", 4, "type", "coding", "points", 40, "topic", "trees"));
        int totalPoints = questions.stream().mapToInt(q -> (int) q.get("points")).sum();
        System.out.println("  [questions] Created " + questions.size() + " questions (" + totalPoints + " points)");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("questions", questions);
        result.getOutputData().put("questionCount", questions.size());
        result.getOutputData().put("assessmentId", "EXAM-677-001");
        return result;
    }
}
