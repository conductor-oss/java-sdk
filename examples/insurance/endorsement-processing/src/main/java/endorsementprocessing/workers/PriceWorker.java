package endorsementprocessing.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class PriceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "edp_price";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [price] Premium adjustment: +$150/year");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("premiumChange", 150);
        result.getOutputData().put("prorated", 75);
        return result;
    }
}
