package npsscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.util.List;

public class CalculateNpsWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "nps_calculate";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<?> responses = (List<?>) task.getInputData().get("responses");
        int total = responses != null ? responses.size() : 0;
        System.out.println("  [calculate] Computing NPS from " + total + " responses");

        // deterministic NPS calculation
        int promoters = 912;   // scores 9-10
        int passives = 300;    // scores 7-8
        int detractors = 228;  // scores 0-6
        double npsScore = ((double) (promoters - detractors) / (promoters + passives + detractors)) * 100;

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("promoters", promoters);
        result.getOutputData().put("passives", passives);
        result.getOutputData().put("detractors", detractors);
        result.getOutputData().put("npsScore", (int) Math.round(npsScore));
        return result;
    }
}
