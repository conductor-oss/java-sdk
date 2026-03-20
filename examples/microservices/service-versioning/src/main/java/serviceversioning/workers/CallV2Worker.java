package serviceversioning.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CallV2Worker implements Worker {
    @Override public String getTaskDefName() { return "sv_call_v2"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [v2] Processing with new API");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("response", "v2-response");
        return r;
    }
}
