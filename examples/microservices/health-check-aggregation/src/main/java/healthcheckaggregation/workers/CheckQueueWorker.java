package healthcheckaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckQueueWorker implements Worker {
    @Override public String getTaskDefName() { return "hc_check_queue"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [queue] Kafka: healthy (lag: 12 messages)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("healthy", true);
        r.getOutputData().put("lag", 12);
        r.getOutputData().put("component", "kafka");
        return r;
    }
}
