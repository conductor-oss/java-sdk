package selfrag.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

/**
 * Grades retrieved documents for relevance.
 * Filters documents with score >= 0.5.
 * Returns {relevantDocs, filteredCount}.
 */
public class GradeDocsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "sr_grade_docs";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> docs = (List<Map<String, Object>>) task.getInputData().get("documents");

        List<Map<String, Object>> relevant = docs.stream()
                .filter(d -> ((Number) d.get("score")).doubleValue() >= 0.5)
                .toList();

        System.out.println("  [grade-docs] " + relevant.size() + "/" + docs.size()
                + " docs relevant (threshold: 0.5)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("relevantDocs", relevant);
        result.getOutputData().put("filteredCount", docs.size() - relevant.size());
        return result;
    }
}
