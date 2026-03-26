package tracecollection.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.HashMap;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;

class InstrumentWorkerTest {
    @Test void taskDefName() { assertEquals("trc_instrument", new InstrumentWorker().getTaskDefName()); }
    @Test void instruments() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("serviceName","test","samplingRate","100%")));
        TaskResult r = new InstrumentWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
        assertEquals(5, r.getOutputData().get("instrumentedServices"));
    }
}
