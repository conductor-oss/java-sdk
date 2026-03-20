package webbrowsing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

/**
 * Plans search queries for a given question.
 * Returns a list of search queries, the search engine to use, and the strategy.
 */
public class PlanSearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "wb_plan_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String question = (String) task.getInputData().get("question");
        if (question == null || question.isBlank()) {
            question = "general information about Conductor";
        }

        System.out.println("  [wb_plan_search] Planning search queries for: " + question);

        List<String> searchQueries = List.of(
                "Conductor workflow engine key features",
                "Conductor orchestration platform production capabilities",
                question
        );

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchQueries", searchQueries);
        result.getOutputData().put("searchEngine", "google");
        result.getOutputData().put("strategy", "multi-query");
        return result;
    }
}
