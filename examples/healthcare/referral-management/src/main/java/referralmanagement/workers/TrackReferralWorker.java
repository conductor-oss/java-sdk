package referralmanagement.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.LinkedHashMap;
import java.util.Map;

public class TrackReferralWorker implements Worker {

    @Override
    public String getTaskDefName() { return "ref_track"; }

    @Override
    public TaskResult execute(Task task) {
        System.out.println("  [track] Tracking referral " + task.getInputData().get("referralId")
                + ", appointment " + task.getInputData().get("appointmentId"));

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        Map<String, Object> output = new LinkedHashMap<>();
        output.put("outcome", "completed");
        output.put("visitCompleted", true);
        output.put("specialistNotes", "Evaluation complete, treatment plan established");
        result.setOutputData(output);
        return result;
    }
}
