package supplierevaluation.workers;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class CollectDataWorkerTest {
    @Test void taskDefName() { assertEquals("spe_collect_data", new CollectDataWorker().getTaskDefName()); }
    @Test void completes() {
        Task t = new Task(); t.setStatus(Task.Status.IN_PROGRESS);
        t.setInputData(new HashMap<>(Map.of("category","raw","period","Q4","suppliers",List.of(Map.of("name","A","deliveryOnTime",0.9,"qualityRate",0.9,"priceIndex",1.0)),"scores",List.of(Map.of("name","A","score",90.0)),"rankings",List.of(Map.of("name","A","score",90.0,"rank",1)))));
        TaskResult r = new CollectDataWorker().execute(t);
        assertEquals(TaskResult.Status.COMPLETED, r.getStatus());
    }
}
