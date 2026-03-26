package usersurvey.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class SurveyReportWorker implements Worker {
    @Override public String getTaskDefName() { return "usv_report"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [report] Survey report generated");
        TaskResult r = new TaskResult(task);
        r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("reportUrl", "https://surveys.example.com/report/" + task.getInputData().get("surveyId"));
        return r;
    }
}
