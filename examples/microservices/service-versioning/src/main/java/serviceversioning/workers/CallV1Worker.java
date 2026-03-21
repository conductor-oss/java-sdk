package serviceversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CallV1Worker implements Worker {
    @Override public String getTaskDefName() { return "sv_call_v1"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [v1] Processing with legacy API");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("response", "v1-response");
        return r;
    }
}
