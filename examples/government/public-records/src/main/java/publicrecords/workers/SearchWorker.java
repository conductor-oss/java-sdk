package publicrecords.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class SearchWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pbr_search";
    }

    @Override
    public TaskResult execute(Task task) {
        String query = (String) task.getInputData().get("query");
        System.out.printf("  [search] Found 3 documents matching \"%s\"%n", query);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("documents", List.of("DOC-A", "DOC-B", "DOC-C"));
        result.getOutputData().put("count", 3);
        return result;
    }
}
