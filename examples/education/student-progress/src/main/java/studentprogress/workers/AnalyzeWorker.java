package studentprogress.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List;
import java.util.Map;

public class AnalyzeWorker implements Worker {
    @Override public String getTaskDefName() { return "spr_analyze"; }

    @Override
    public TaskResult execute(Task task) {
        double gpa = 3.52;
        String standing = gpa >= 3.5 ? "Dean's List" : gpa >= 2.0 ? "Good Standing" : "Academic Probation";
        System.out.println("  [analyze] GPA: " + gpa + " - " + standing);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("gpa", gpa);
        result.getOutputData().put("standing", standing);
        result.getOutputData().put("analysis", Map.of(
                "strongAreas", List.of("CS"),
                "needsImprovement", List.of("PHYS")));
        return result;
    }
}
