package timezonehandling.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ScheduleJobWorker implements Worker {
    @Override public String getTaskDefName() { return "tz_schedule_job"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [schedule] Scheduling \"" + task.getInputData().get("jobName") + "\" at " + task.getInputData().get("utcTime"));
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("scheduled", true);
        r.getOutputData().put("jobId", "tz-job-7project-kickoff");
        return r;
    }
}
