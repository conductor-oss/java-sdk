package ddosmitigation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ClassifyWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ddos_classify";
    }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [classify] Layer 7 HTTP flood attack from botnet");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.addOutputData("classify", true);
        result.addOutputData("processed", true);
        return result;
    }
}
