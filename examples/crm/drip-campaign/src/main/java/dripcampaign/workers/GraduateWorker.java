package dripcampaign.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class GraduateWorker implements Worker {
    @Override public String getTaskDefName() { return "drp_graduate"; }

    @Override
    public TaskResult execute(Task task) {
        int engagement = task.getInputData().get("engagement") instanceof Number n ? n.intValue() : 0;
        boolean graduated = engagement >= 60;
        System.out.println("  [graduate] Contact " + task.getInputData().get("contactId") + ": " + (graduated ? "graduated to sales" : "recycled"));
        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("graduated", graduated);
        result.getOutputData().put("nextAction", graduated ? "sales_handoff" : "recycle_nurture");
        return result;
    }
}
