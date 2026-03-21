package performancereview.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class CalibrateWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "pfr_calibrate";
    }

    @Override
    public TaskResult execute(Task task) {
        double selfRating = Double.parseDouble(String.valueOf(task.getInputData().get("selfRating")));
        double managerRating = Double.parseDouble(String.valueOf(task.getInputData().get("managerRating")));
        double avg = (selfRating + managerRating) / 2.0;
        String finalRating = String.format("%.1f", avg);

        System.out.println("  [calibrate] Calibrated rating: " + finalRating +
                " (self: " + selfRating + ", mgr: " + managerRating + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("finalRating", finalRating);
        result.getOutputData().put("band", "exceeds-expectations");
        return result;
    }
}
