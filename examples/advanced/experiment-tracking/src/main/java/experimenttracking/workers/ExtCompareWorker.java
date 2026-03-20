package experimenttracking.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class ExtCompareWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "ext_compare";
    }

    @Override
    public TaskResult execute(Task task) {
        double current = task.getInputData().get("currentMetric") instanceof Number ? ((Number) task.getInputData().get("currentMetric")).doubleValue() : 0;
        double baseline = task.getInputData().get("baselineMetric") instanceof Number ? ((Number) task.getInputData().get("baselineMetric")).doubleValue() : 0;
        double improvement = baseline > 0 ? ((current - baseline) / baseline * 100) : 0;
        boolean significant = Math.abs(current - baseline) > 0.01;
        System.out.println("  [compare] Processing");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("improvement", String.format("%.2f%%", improvement));
        result.getOutputData().put("statSignificant", significant);
        return result;
    }
}