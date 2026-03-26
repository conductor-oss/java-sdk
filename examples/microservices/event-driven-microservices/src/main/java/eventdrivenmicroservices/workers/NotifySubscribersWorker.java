package eventdrivenmicroservices.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;

public class NotifySubscribersWorker implements Worker {
    @Override public String getTaskDefName() { return "edm_notify_subscribers"; }
    @Override @SuppressWarnings("unchecked")
    public TaskResult execute(Task task) {
        Object subs = task.getInputData().get("subscribers");
        int count = subs instanceof List ? ((List<?>) subs).size() : 0;
        System.out.println("  [notify] Notified " + count + " subscribers");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("count", count);
        return r;
    }
}
