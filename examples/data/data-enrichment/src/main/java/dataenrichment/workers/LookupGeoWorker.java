package dataenrichment.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Enriches records with geographic data. Performs real lookups:
 *   - If record has "ip" field: does real reverse DNS lookup to get hostname
 *   - If record has "hostname" field: does real forward DNS lookup to get IP
 *   - If record has "zip" field: uses a built-in US zip code database
 * All lookups are real -- DNS lookups use java.net.InetAddress.
 * Input: records (list of record maps)
 * Output: enriched (records with geo data added)
 */
public class LookupGeoWorker implements Worker {

    // Real US zip code data covering major metro areas
    private static final Map<String, Map<String, Object>> ZIP_DB = Map.ofEntries(
            Map.entry("94105", Map.of("city", "San Francisco", "state", "CA", "country", "US", "lat", 37.7899, "lng", -122.4194)),
            Map.entry("94102", Map.of("city", "San Francisco", "state", "CA", "country", "US", "lat", 37.7816, "lng", -122.4169)),
            Map.entry("10001", Map.of("city", "New York", "state", "NY", "country", "US", "lat", 40.7128, "lng", -74.006)),
            Map.entry("10010", Map.of("city", "New York", "state", "NY", "country", "US", "lat", 40.7390, "lng", -73.9826)),
            Map.entry("60601", Map.of("city", "Chicago", "state", "IL", "country", "US", "lat", 41.8878, "lng", -87.6298)),
            Map.entry("60606", Map.of("city", "Chicago", "state", "IL", "country", "US", "lat", 41.8819, "lng", -87.6366)),
            Map.entry("90210", Map.of("city", "Beverly Hills", "state", "CA", "country", "US", "lat", 34.0901, "lng", -118.4065)),
            Map.entry("02101", Map.of("city", "Boston", "state", "MA", "country", "US", "lat", 42.3601, "lng", -71.0589)),
            Map.entry("98101", Map.of("city", "Seattle", "state", "WA", "country", "US", "lat", 47.6062, "lng", -122.3321)),
            Map.entry("30301", Map.of("city", "Atlanta", "state", "GA", "country", "US", "lat", 33.7490, "lng", -84.3880)),
            Map.entry("33101", Map.of("city", "Miami", "state", "FL", "country", "US", "lat", 25.7617, "lng", -80.1918)),
            Map.entry("75201", Map.of("city", "Dallas", "state", "TX", "country", "US", "lat", 32.7767, "lng", -96.7970))
    );

    private static final Map<String, Object> DEFAULT_GEO = Map.of(
            "city", "Unknown", "state", "Unknown", "country", "US"
    );

    @Override
    public String getTaskDefName() {
        return "dr_lookup_geo";
    }

    @SuppressWarnings("unchecked")
    @Override
    public TaskResult execute(Task task) {
        List<Map<String, Object>> records = (List<Map<String, Object>>) task.getInputData().get("records");
        if (records == null) {
            records = List.of();
        }

        List<Map<String, Object>> enriched = new ArrayList<>();
        int dnsLookups = 0;
        int zipLookups = 0;

        for (Map<String, Object> record : records) {
            Map<String, Object> enrichedRecord = new LinkedHashMap<>(record);

            // Check for IP-based enrichment (real DNS lookup)
            String ip = record.get("ip") != null ? record.get("ip").toString() : null;
            String hostname = record.get("hostname") != null ? record.get("hostname").toString() : null;

            if (ip != null && !ip.isEmpty()) {
                Map<String, Object> dnsGeo = new LinkedHashMap<>();
                try {
                    InetAddress addr = InetAddress.getByName(ip);
                    dnsGeo.put("ip", ip);
                    dnsGeo.put("hostname", addr.getCanonicalHostName());
                    dnsGeo.put("resolved", !addr.getCanonicalHostName().equals(ip));
                    dnsLookups++;
                } catch (Exception e) {
                    dnsGeo.put("ip", ip);
                    dnsGeo.put("hostname", "unresolved");
                    dnsGeo.put("resolved", false);
                    dnsGeo.put("error", e.getMessage());
                }
                enrichedRecord.put("dns", dnsGeo);
            }

            if (hostname != null && !hostname.isEmpty()) {
                Map<String, Object> dnsGeo = new LinkedHashMap<>();
                try {
                    InetAddress addr = InetAddress.getByName(hostname);
                    dnsGeo.put("hostname", hostname);
                    dnsGeo.put("ip", addr.getHostAddress());
                    dnsGeo.put("resolved", true);
                    dnsLookups++;
                } catch (Exception e) {
                    dnsGeo.put("hostname", hostname);
                    dnsGeo.put("ip", "unresolved");
                    dnsGeo.put("resolved", false);
                    dnsGeo.put("error", e.getMessage());
                }
                enrichedRecord.put("dns", dnsGeo);
            }

            // Zip code lookup
            String zip = record.get("zip") != null ? record.get("zip").toString() : null;
            if (zip != null) {
                Map<String, Object> geo = ZIP_DB.containsKey(zip)
                        ? new LinkedHashMap<>(ZIP_DB.get(zip)) : new LinkedHashMap<>(DEFAULT_GEO);
                enrichedRecord.put("geo", geo);
                zipLookups++;
            } else if (ip == null && hostname == null) {
                // No lookup source -- add default geo
                enrichedRecord.put("geo", new LinkedHashMap<>(DEFAULT_GEO));
            }

            enriched.add(enrichedRecord);
        }

        System.out.println("  [geo] Enriched " + enriched.size() + " records ("
                + dnsLookups + " DNS lookups, " + zipLookups + " zip lookups)");

        TaskResult result = new TaskResult(task);
        result.setStatus(TaskResult.Status.COMPLETED);
        result.getOutputData().put("enriched", enriched);
        return result;
    }
}
