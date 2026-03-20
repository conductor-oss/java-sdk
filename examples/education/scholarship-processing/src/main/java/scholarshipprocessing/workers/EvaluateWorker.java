package scholarshipprocessing.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class EvaluateWorker implements Worker {
    @Override public String getTaskDefName() { return "scp_evaluate"; }
    @Override public TaskResult execute(Task task) {
        double gpa = 0;
        Object gpaObj = task.getInputData().get("gpa");
        if (gpaObj instanceof Number) gpa = ((Number) gpaObj).doubleValue();
        else if (gpaObj instanceof String) { try { gpa = Double.parseDouble((String) gpaObj); } catch (NumberFormatException ignored) {} }
        String need = (String) task.getInputData().getOrDefault("financialNeed", "low");
        int needScore = "high".equals(need) ? 30 : "medium".equals(need) ? 20 : 10;
        int score = (int) Math.round(gpa * 17.5 + needScore);
        System.out.println("  [evaluate] " + task.getInputData().get("applicationId") + ": GPA " + gpa + ", need " + need + " -> score " + score);
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("score", score);
        result.getOutputData().put("eligible", score >= 70);
        return result;
    }
}
