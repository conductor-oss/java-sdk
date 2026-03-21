package customerchurn.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CreateOfferWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ccn_create_offer";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [offer] Retention offer: 20%% discount for 6 months");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("offerId", "OFR-customer-churn-001");
        result.getOutputData().put("offer", java.util.Map.of("discount", 20, "duration", "6 months", "type", "loyalty"));
        return result;
    }
}
