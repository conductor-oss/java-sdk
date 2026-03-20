package disasterrecovery;

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
        assertEquals("dr_detect", new disasterrecovery.workers.DetectWorker().getTaskDefName());
        assertEquals("dr_failover_db", new disasterrecovery.workers.FailoverDbWorker().getTaskDefName());
        assertEquals("dr_update_dns", new disasterrecovery.workers.UpdateDnsWorker().getTaskDefName());
        assertEquals("dr_verify", new disasterrecovery.workers.VerifyWorker().getTaskDefName());
    }
}
