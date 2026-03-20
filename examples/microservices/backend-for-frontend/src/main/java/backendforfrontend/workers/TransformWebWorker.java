package backendforfrontend.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class TransformWebWorker implements Worker {
    @Override public String getTaskDefName() { return "bff_transform_web"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [web] Full desktop response with all fields");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("format", "web");
        r.getOutputData().put("fields", "full");
        r.getOutputData().put("data", task.getInputData().get("data"));
        return r;
    }
}
