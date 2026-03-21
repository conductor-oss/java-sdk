package gradingworkflow.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

public class RecordWorker implements Worker {

    @Override
    public String getTaskDefName() {
        return "grd_record";
    }

    @Override
    public TaskResult execute(Task task) {
        String studentId = (String) task.getInputData().get("studentId");
        String courseId = (String) task.getInputData().get("courseId");
        Object finalScore = task.getInputData().get("finalScore");

        System.out.println("  [record] Score " + finalScore + " recorded for " + studentId + " in " + courseId);

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("recorded", true);
        return result;
    }
}
