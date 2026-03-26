package sharednothingarchitecture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServiceBWorker implements Worker {
    @Override public String getTaskDefName() { return "sn_service_b"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [service-b] Processing with a's output only");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "b-processed");
        r.getOutputData().put("instanceId", "b-1");
        return r;
    }
}
