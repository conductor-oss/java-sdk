package insurancerenewal.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RepriceWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "irn_reprice";
    }

    @Override
    public TaskResult execute(Task task) {

        System.out.println("  [reprice] Premium adjusted: $1,200/year — renewal recommended");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("decision", "renew");
        result.getOutputData().put("newPremium", 1200);
        return result;
    }
}
