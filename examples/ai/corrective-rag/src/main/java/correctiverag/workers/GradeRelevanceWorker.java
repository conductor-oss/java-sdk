package correctiverag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Grades retrieved documents for relevance. Averages relevance scores
 * and returns "relevant" (avg >= 0.5) or "irrelevant" (avg < 0.5).
 */
public class GradeRelevanceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cr_grade_relevance";
    }

    @Override
    @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        List<Map<String, Object>> documents = (List<Map<String, Object>>) task.getInputData().get("documents");

        double sum = 0.0;
        int count = 0;
        if (documents != null) {
            for (Map<String, Object> doc : documents) {
                Object rel = doc.get("relevance");
                if (rel instanceof Number) {
                    sum += ((Number) rel).doubleValue();
                    count++;
                }
            }
        }

        double avg = count > 0 ? sum / count : 0.0;
        String verdict = avg >= 0.5 ? "relevant" : "irrelevant";

        String avgFormatted = String.format("%.2f", avg);
        System.out.println("  [grade] Average relevance: " + avgFormatted + " -> verdict: " + verdict);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("verdict", verdict);
        result.getOutputData().put("avgScore", avgFormatted);
        return result;
    }
}
