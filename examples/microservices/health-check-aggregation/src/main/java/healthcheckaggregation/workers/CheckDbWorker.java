package healthcheckaggregation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CheckDbWorker implements Worker {
    @Override public String getTaskDefName() { return "hc_check_db"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [db] Database: healthy (connections: 23/100)");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("healthy", true);
        r.getOutputData().put("connections", 23);
        r.getOutputData().put("component", "postgres");
        return r;
    }
}
