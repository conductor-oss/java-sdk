package actuarialworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CollectDataWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "act_collect_data";
    }

    @Override
    public TaskResult execute(Task task) {

        String lineOfBusiness = (String) task.getInputData().get("lineOfBusiness");
        System.out.printf("  [collect] Historical data for %s%n", lineOfBusiness);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("dataSet", java.util.Map.of("records", 45000, "years", 10));
        return result;
    }
}
