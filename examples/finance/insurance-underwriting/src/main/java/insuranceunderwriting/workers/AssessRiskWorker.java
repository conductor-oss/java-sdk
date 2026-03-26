package insuranceunderwriting.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.Map;
public class AssessRiskWorker implements Worker {
    @Override public String getTaskDefName() { return "uw_assess_risk"; }
    @SuppressWarnings("unchecked")
    @Override public TaskResult execute(Task task) {
        Map<String, Object> data = (Map<String, Object>) task.getInputData().get("applicantData");
        if (data == null) data = Map.of();
        int age = data.get("age") instanceof Number ? ((Number) data.get("age")).intValue() : 50;
        boolean smoker = Boolean.TRUE.equals(data.get("smoker"));
        double bmi = data.get("bmi") instanceof Number ? ((Number) data.get("bmi")).doubleValue() : 25;
        String riskClass = "standard"; String decision = "accept";
        if (age < 40 && !smoker && bmi < 30) { riskClass = "preferred"; decision = "accept"; }
        else if (smoker || bmi > 35) { riskClass = "substandard"; Object amtObj = task.getInputData().get("coverageAmount"); double amt = amtObj instanceof Number ? ((Number) amtObj).doubleValue() : 0; decision = amt > 1000000 ? "refer" : "accept"; }
        System.out.println("  [risk] Class: " + riskClass + ", Decision: " + decision);
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("riskClass", riskClass); r.getOutputData().put("decision", decision);
        r.getOutputData().put("riskScore", 22); r.getOutputData().put("declineReason", "");
        r.getOutputData().put("referReason", "refer".equals(decision) ? "High coverage + risk factors" : "");
        return r;
    }
}
