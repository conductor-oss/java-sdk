package auditlogging;

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
        assertEquals("al_capture_event", new auditlogging.workers.CaptureEventWorker().getTaskDefName());
        assertEquals("al_enrich_context", new auditlogging.workers.EnrichContextWorker().getTaskDefName());
        assertEquals("al_store_immutable", new auditlogging.workers.StoreImmutableWorker().getTaskDefName());
        assertEquals("al_verify_integrity", new auditlogging.workers.VerifyIntegrityWorker().getTaskDefName());
    }
}
