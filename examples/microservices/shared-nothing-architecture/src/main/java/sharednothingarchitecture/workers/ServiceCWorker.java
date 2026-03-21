package sharednothingarchitecture.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ServiceCWorker implements Worker {
    @Override public String getTaskDefName() { return "sn_service_c"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [service-c] Processing with b's output only");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("result", "c-processed");
        r.getOutputData().put("instanceId", "c-1");
        return r;
    }
}
