package taxassessment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

public class CollectDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "txa_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {
        String propertyId = (String) task.getInputData().get("propertyId");
        System.out.printf("  [collect] Gathered data for property %s%n", propertyId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("propertyData", Map.of(
                "sqft", 2400,
                "bedrooms", 4,
                "lotSize", 0.25
        ));
        return result;
    }
}
