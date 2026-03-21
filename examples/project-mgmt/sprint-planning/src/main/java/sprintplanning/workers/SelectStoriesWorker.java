package sprintplanning.workers;
import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import java.util.List; import java.util.Map;
public class SelectStoriesWorker implements Worker {
    @Override public String getTaskDefName() { return "spn_select_stories"; }
    @Override public TaskResult execute(Task task) {
        System.out.println("  [stories] Selecting stories for capacity " + task.getInputData().get("teamCapacity") + " points");
        TaskResult r = new TaskResult(task); r.setStatus(TaskResult.Status.COMPLETED);
        r.getOutputData().put("stories", List.of(Map.of("id","US-101","title","User login","priority","high"),Map.of("id","US-102","title","Dashboard view","priority","medium"),Map.of("id","US-103","title","Export CSV","priority","low"))); return r;
    }
}
