package trainingmanagement.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "trm_track"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [track] Enrollment " + task.getInputData().get("enrollmentId") + ": 100% modules completed");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("progress", 100);
        r.getOutputData().put("modulesCompleted", 8);
        r.getOutputData().put("totalModules", 8);
        return r;
    }
}
