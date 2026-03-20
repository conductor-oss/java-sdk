package monitoringalerting;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MainExampleTest {

    @Test
    void workflowJsonIsLoadable() {
        var is = getClass().getClassLoader().getResourceAsStream("workflow.json");
        assertNotNull(is, "workflow.json should be loadable from resources");
    }

    @Test
    void workerInstantiation() {
        var evaluate = new monitoringalerting.workers.EvaluateWorker();
        assertEquals("ma_evaluate", evaluate.getTaskDefName());

        var dedup = new monitoringalerting.workers.DeduplicateWorker();
        assertEquals("ma_deduplicate", dedup.getTaskDefName());

        var enrich = new monitoringalerting.workers.EnrichWorker();
        assertEquals("ma_enrich", enrich.getTaskDefName());

        var route = new monitoringalerting.workers.RouteWorker();
        assertEquals("ma_route", route.getTaskDefName());
    }
}
