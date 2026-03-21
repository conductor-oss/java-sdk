package threatintelligence;

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
        assertEquals("ti_ingest_feeds", new threatintelligence.workers.IngestFeedsWorker().getTaskDefName());
        assertEquals("ti_correlate_iocs", new threatintelligence.workers.CorrelateIocsWorker().getTaskDefName());
        assertEquals("ti_enrich_context", new threatintelligence.workers.EnrichContextWorker().getTaskDefName());
        assertEquals("ti_distribute_intel", new threatintelligence.workers.DistributeIntelWorker().getTaskDefName());
    }
}
