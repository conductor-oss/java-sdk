package salvagerecovery.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class AuctionWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "slv_auction";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [auction] Sold for $5,100");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("proceeds", 5100);
        result.getOutputData().put("buyer", "SALVAGE-BUYER-7");
        result.getOutputData().put("bids", 8);
        return result;
    }
}
