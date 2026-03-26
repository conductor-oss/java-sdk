package crmagent.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.Map;

/**
 * Looks up a customer by ID and returns their profile information
 * including name, email, tier, account age, contract value, assigned rep, and industry.
 */
public class LookupCustomerWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "cm_lookup_customer";
    }

    @Override
    public TaskResult execute(Task task) {
        String customerId = (String) task.getInputData().get("customerId");
        if (customerId == null || customerId.isBlank()) {
            customerId = "UNKNOWN";
        }

        String channel = (String) task.getInputData().get("channel");
        if (channel == null || channel.isBlank()) {
            channel = "unknown";
        }

        System.out.println("  [cm_lookup_customer] Looking up customer: " + customerId
                + " via " + channel);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("name", "Acme Corporation");
        result.getOutputData().put("email", "support@acme.com");
        result.getOutputData().put("tier", "enterprise");
        result.getOutputData().put("accountAge", "3 years");
        result.getOutputData().put("contractValue", "$250,000/year");
        result.getOutputData().put("assignedRep", "Michael Torres");
        result.getOutputData().put("industry", "manufacturing");
        return result;
    }
}
