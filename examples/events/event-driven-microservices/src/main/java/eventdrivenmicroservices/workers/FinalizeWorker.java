package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Finalizes the event-driven microservices workflow.
 * Input: orderId, eventsEmitted, servicesInvolved
 * Output: servicesCompleted (<servicesInvolved>), eventsPublished (<eventsEmitted>), status ("completed")
 */
public class FinalizeWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "dm_finalize";
    }

    @Override
    public TaskResult execute(Task task) {
        String orderId = task.getInputData().get("orderId") != null
                ? String.valueOf(task.getInputData().get("orderId"))
                : "unknown";

        Object eventsEmitted = task.getInputData().get("eventsEmitted");
        if (eventsEmitted == null) {
            eventsEmitted = 0;
        }

        Object servicesInvolved = task.getInputData().get("servicesInvolved");
        if (servicesInvolved == null) {
            servicesInvolved = 0;
        }

        System.out.println("  [finalize] Order " + orderId + " completed: "
                + servicesInvolved + " services, " + eventsEmitted + " events");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("servicesCompleted", servicesInvolved);
        result.getOutputData().put("eventsPublished", eventsEmitted);
        result.getOutputData().put("status", "completed");
        return result;
    }
}
