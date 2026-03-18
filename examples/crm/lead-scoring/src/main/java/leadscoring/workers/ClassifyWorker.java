package leadscoring.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

/**
 * Classifies lead based on score. Real classification with tier assignment.
 */
public class ClassifyWorker implements Worker {
    @Override public String getTaskDefName() { return "ls_classify"; }

    @Override public TaskResult execute(Task task) {
        Object scoreObj = task.getInputData().get("totalScore");
        int score = scoreObj instanceof Number ? ((Number) scoreObj).intValue() : 0;

        String classification;
        String priority;
        if (score >= 80) { classification = "hot"; priority = "P1"; }
        else if (score >= 60) { classification = "warm"; priority = "P2"; }
        else if (score >= 40) { classification = "cool"; priority = "P3"; }
        else { classification = "cold"; priority = "P4"; }

        System.out.println("  [classify] Score " + score + " -> " + classification + " (" + priority + ")");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("classification", classification);
        result.getOutputData().put("priority", priority);
        return result;
    }
}
