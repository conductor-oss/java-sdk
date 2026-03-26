package dataexportrequest.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;
import java.util.Map;

public class CollectDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "der_collect";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<String> categories = (List<String>) task.getInputData().get("categories");
        if (categories == null) categories = List.of();
        System.out.println("  [collect] Collected data from " + categories.size() + " categories");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("collectedData", Map.of("profile", Map.of(), "activity", List.of(), "purchases", List.of()));
        result.getOutputData().put("totalRecords", 1247);
        return result;
    }
}
