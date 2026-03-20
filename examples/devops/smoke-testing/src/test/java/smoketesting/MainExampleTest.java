package smoketesting;

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
        assertEquals("st_check_endpoints", new smoketesting.workers.CheckEndpointsWorker().getTaskDefName());
        assertEquals("st_verify_data", new smoketesting.workers.VerifyDataWorker().getTaskDefName());
        assertEquals("st_test_integrations", new smoketesting.workers.TestIntegrationsWorker().getTaskDefName());
        assertEquals("st_report_status", new smoketesting.workers.ReportStatusWorker().getTaskDefName());
    }
}
