package interviewscheduling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class ScheduleWorker implements Worker {
    @Override public String getTaskDefName() { return "ivs_schedule"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Selected optimal interview slot");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("interviewId", "INT-603");
        r.getOutputData().put("scheduledTime", "2024-03-25 10:00");
        r.getOutputData().put("room", "Conference B");
        return r;
    }
}
