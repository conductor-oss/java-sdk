package circuitbreakermicroservice.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordResultWorker implements Worker {
    @Override public String getTaskDefName() { return "cb_record_result"; }
    @Override public TaskResult execute(Task task) {
        String svc = (String) task.getInputData().getOrDefault("serviceName", "unknown");
        System.out.println("  [record] Success recorded for " + svc);
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("recorded", true);
        return r;
    }
}
