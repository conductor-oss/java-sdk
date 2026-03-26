package accessreview;

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
        assertEquals("ar_collect_entitlements", new accessreview.workers.CollectEntitlementsWorker().getTaskDefName());
        assertEquals("ar_identify_anomalies", new accessreview.workers.IdentifyAnomaliesWorker().getTaskDefName());
        assertEquals("ar_request_certification", new accessreview.workers.RequestCertificationWorker().getTaskDefName());
        assertEquals("ar_enforce_decisions", new accessreview.workers.EnforceDecisionsWorker().getTaskDefName());
    }
}
