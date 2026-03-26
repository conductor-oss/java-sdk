package trademarksearch.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SearchWorker implements Worker {
    @Override public String getTaskDefName() { return "tmk_search"; }

    @Override public TaskResult execute(Task task) {
        String name = (String) task.getInputData().get("trademarkName");
        System.out.println("  [search] Searching trademark databases for: " + name);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("searchResults", java.util.List.of("SimilarMark-001", "SimilarMark-002"));
        result.getOutputData().put("totalResults", 2);
        return result;
    }
}
