package demandforecasting.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class CollectDataWorkerTest {
    @Test void taskDefName() { assertEquals("df_collect_data", new CollectDataWorker().getTaskDefName()); }
    @Test void completesSuccessfully() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("productCategory","electronics","region","NA","horizon","6-month","historicalData",List.of(),"trends",Map.of(),"forecast",Map.of("total",10000))));
        TaskResult r = new CollectDataWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
