package circuitbreakermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckCircuitWorker implements Worker {
    @Override public String getTaskDefName() { return "cb_check_circuit"; }
    @Override public TaskResult execute(Task task) {
        String svc = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [circuit] Checking state for " + svc + ": CLOSED");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("circuitState", "closed");
        r.getOutputData().put("failureCount", 0);
        r.getOutputData().put("threshold", 5);
        return r;
    }
}
