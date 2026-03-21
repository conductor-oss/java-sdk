package recruitmentpipeline.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap; import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
class PostWorkerTest {
    private final PostWorker w = new PostWorker();
    @Test void taskDefName() { assertEquals("rcp_post", w.getTaskDefName()); }
    @Test void executes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("jobTitle","SWE","candidateName","Alex","department","Eng","jobId","JOB-602","screenScore","85","interviewScore","4.2","recommendation","hire")));
        assertEquals(TaskResult.Status.COMPLETED, w.execute(t).getStatus());
    }
}
