package bidmanagement.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CollectWorkerTest {
    @Test void taskDefName() { assertEquals("bid_collect", new CollectWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("projectName","Test","budget",100000,"vendors",List.of("A","B"),"bidId","BID-001","responses",List.of(Map.of("vendor","A","amount",80000,"timeline","8 weeks")),"winner","A")));
        TaskResult r = new CollectWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
