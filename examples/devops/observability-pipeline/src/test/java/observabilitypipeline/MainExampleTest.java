package observabilitypipeline;

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
        assertEquals("op_collect_metrics", new observabilitypipeline.workers.CollectMetricsWorker().getTaskDefName());
        assertEquals("op_correlate_traces", new observabilitypipeline.workers.CorrelateTracesWorker().getTaskDefName());
        assertEquals("op_detect_anomalies", new observabilitypipeline.workers.DetectAnomaliesWorker().getTaskDefName());
        assertEquals("op_alert_or_store", new observabilitypipeline.workers.AlertOrStoreWorker().getTaskDefName());
    }
}
