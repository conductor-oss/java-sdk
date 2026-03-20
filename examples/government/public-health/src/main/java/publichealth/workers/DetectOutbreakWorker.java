package publichealth.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class DetectOutbreakWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "phw_detect_outbreak";
    }

    @Override
    public TaskResult execute(Task task) {
        int cases = 0;
        int baseline = 10;
        Object casesObj = task.getInputData().get("caseCount");
        if (casesObj instanceof Number) cases = ((Number) casesObj).intValue();
        Object baselineObj = task.getInputData().get("baseline");
        if (baselineObj instanceof Number) baseline = ((Number) baselineObj).intValue();

        boolean isOutbreak = cases > baseline * 2;
        System.out.printf("  [detect] Cases: %d, baseline: %d — %s%n", cases, baseline, isOutbreak ? "OUTBREAK" : "normal");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("action", isOutbreak ? "alert" : "monitor");
        result.getOutputData().put("severity", isOutbreak ? "high" : "low");
        result.getOutputData().put("nextCheck", "2024-03-17");
        return result;
    }
}
