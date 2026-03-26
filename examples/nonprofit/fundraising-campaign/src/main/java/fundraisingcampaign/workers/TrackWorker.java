package fundraisingcampaign.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
public class TrackWorker implements Worker {
    @Override public String getTaskDefName() { return "frc_track"; }
    @Override public TaskResult execute(Task task) {
        int goal = 100000; Object ga = task.getInputData().get("goalAmount");
        if (ga instanceof Number) goal = ((Number) ga).intValue();
        int raised = (int)(goal * 1.12);
        System.out.println("  [track] Raised $" + raised + " of $" + goal + " goal");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.addOutputData("raised", raised); r.addOutputData("donors", 342); r.addOutputData("avgDonation", raised / 342); return r;
    }
}
