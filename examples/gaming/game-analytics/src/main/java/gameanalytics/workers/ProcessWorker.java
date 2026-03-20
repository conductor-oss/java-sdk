package gameanalytics.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class ProcessWorker implements Worker {
    @Override public String getTaskDefName() { return "gan_process"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [process] Processing " + task.getInputData().get("rawEvents") + " raw events");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("processed", Map.of("sessions", 8500, "matches", 4200, "purchases", 320));
        return r;
    }
}
