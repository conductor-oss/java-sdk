package menumanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;

public class CategorizeWorker implements Worker {
    @Override public String getTaskDefName() { return "mnu_categorize"; }

    @Override public TaskResult execute(Task task) {
        System.out.println("  [categorize] Organizing items into categories");
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("menu", Map.of("entrees", 2, "desserts", 1, "totalItems", 3));
        return result;
    }
}
