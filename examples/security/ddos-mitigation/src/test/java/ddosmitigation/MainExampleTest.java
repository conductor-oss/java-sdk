package ddosmitigation;

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
        assertEquals("ddos_detect", new ddosmitigation.workers.DetectWorker().getTaskDefName());
        assertEquals("ddos_classify", new ddosmitigation.workers.ClassifyWorker().getTaskDefName());
        assertEquals("ddos_mitigate", new ddosmitigation.workers.MitigateWorker().getTaskDefName());
        assertEquals("ddos_verify_service", new ddosmitigation.workers.VerifyServiceWorker().getTaskDefName());
    }
}
