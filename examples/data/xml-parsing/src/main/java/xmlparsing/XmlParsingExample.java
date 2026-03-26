package xmlparsing;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.run.Workflow;
import xmlparsing.workers.ReceiveXmlWorker;
import xmlparsing.workers.ParseTagsWorker;
import xmlparsing.workers.ExtractFieldsWorker;
import xmlparsing.workers.ConvertToJsonWorker;
import xmlparsing.workers.EmitRecordsWorker;

import java.util.List;
import java.util.Map;

/**
 * XML Parsing Workflow Demo
 *
 * Demonstrates a sequential XML parsing pipeline:
 *   xp_receive_xml -> xp_parse_tags -> xp_extract_fields -> xp_convert_to_json -> xp_emit_records
 *
 * Run:
 *   java -jar target/xml-parsing-1.0.0.jar
 */
public class XmlParsingExample {

    public static void main(String[] args) throws Exception {
        boolean workersOnly = args.length > 0 && "--workers".equals(args[0]);

        System.out.println("=== XML Parsing Workflow Demo ===\n");

        var client = new ConductorClientHelper();

        // Step 1 -- Register task definitions
        System.out.println("Step 1: Registering task definitions...");
        client.registerTaskDefs(List.of(
                "xp_receive_xml", "xp_parse_tags",
                "xp_extract_fields", "xp_convert_to_json", "xp_emit_records"));
        System.out.println("  Registered: xp_receive_xml, xp_parse_tags, xp_extract_fields, xp_convert_to_json, xp_emit_records\n");

        // Step 2 -- Register workflow
        System.out.println("Step 2: Registering workflow 'xml_parsing_wf'...");
        client.registerWorkflow("workflow.json");
        System.out.println("  Workflow registered.\n");

        // Step 3 -- Start workers
        System.out.println("Step 3: Starting workers...");
        List<Worker> workers = List.of(
                new ReceiveXmlWorker(),
                new ParseTagsWorker(),
                new ExtractFieldsWorker(),
                new ConvertToJsonWorker(),
                new EmitRecordsWorker()
        );
        client.startWorkers(workers);
        System.out.println("  5 workers polling.\n");

        if (workersOnly) {
            System.out.println("Running in worker-only mode. Use the Conductor CLI to start workflows.");
            System.out.println("Press Ctrl+C to stop.\n");
            Thread.currentThread().join();
            return;
        }

        Thread.sleep(2000);

        // Step 4 -- Start the workflow
        System.out.println("Step 4: Starting workflow...\n");
        String workflowId = client.startWorkflow("xml_parsing_wf", 1,
                Map.of("xmlContent", "<products><product id=\"P-101\"><name>Laptop</name></product></products>",
                        "rootElement", "products"));
        System.out.println("  Workflow ID: " + workflowId + "\n");

        // Step 5 -- Wait for completion
        System.out.println("Step 5: Waiting for completion...");
        Workflow workflow = client.waitForWorkflow(workflowId, "COMPLETED", 60000);
        String status = workflow.getStatus().name();
        System.out.println("  Status: " + status);
        System.out.println("  Output: " + workflow.getOutput());

        client.stopWorkers();

        if ("COMPLETED".equals(status)) {
            System.out.println("\nResult: PASSED");
            System.exit(0);
        } else {
            System.out.println("\nResult: FAILED");
            System.exit(1);
        }
    }
}
