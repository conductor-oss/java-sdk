package certificaterotation.workers;

import com.netflix.conductor.client.worker.Worker;
import com.netflix.conductor.common.metadata.tasks.Task;
import com.netflix.conductor.common.metadata.tasks.TaskResult;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.Signature;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;

/**
 * Generates a new self-signed X.509 certificate for the target domain.
 * Uses Java's KeyPairGenerator to create a real RSA key pair and builds
 * a DER-encoded self-signed certificate from scratch.
 *
 * In production you would submit a CSR to a real CA (Let's Encrypt,
 * AWS ACM, etc.). This example generates a real, cryptographically valid
 * self-signed certificate to demonstrate the workflow.
 *
 * Input:
 *   - generateData.domain (String): domain for the certificate CN
 *
 * Output:
 *   - certPem (String): PEM-encoded certificate
 *   - serialNumber (String): certificate serial number
 *   - algorithm (String): key algorithm used
 *   - keySize (int): key size in bits
 *   - validDays (int): certificate validity period
 *   - domain (String): domain the cert was generated for
 */
public class GenerateWorker implements Worker {

    private static final int KEY_SIZE = 2048;
    private static final int VALID_DAYS = 365;

    @Override
    public String getTaskDefName() {
        return "cr_generate";
    }

    @Override
    public TaskResult execute(Task task) {
        String domain = "localhost";
        Object generateData = task.getInputData().get("generateData");
        if (generateData instanceof java.util.Map) {
            @SuppressWarnings("unchecked")
            java.util.Map<String, Object> data = (java.util.Map<String, Object>) generateData;
            if (data.get("domain") != null) {
                domain = data.get("domain").toString();
            }
        }

        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            keyGen.initialize(KEY_SIZE, new SecureRandom());
            KeyPair keyPair = keyGen.generateKeyPair();

            BigInteger serial = BigInteger.valueOf(System.currentTimeMillis());
            Date notBefore = new Date();
            Date notAfter = Date.from(Instant.now().plus(VALID_DAYS, ChronoUnit.DAYS));

            // Build a self-signed X.509v3 certificate using raw DER encoding
            byte[] certDer = buildSelfSignedCert(domain, serial, notBefore, notAfter, keyPair);

            String pem = "-----BEGIN CERTIFICATE-----\n"
                    + Base64.getMimeEncoder(64, "\n".getBytes()).encodeToString(certDer)
                    + "\n-----END CERTIFICATE-----";

            System.out.println("  [generate] Created " + KEY_SIZE + "-bit RSA cert for CN=" + domain
                    + " (serial: " + serial + ", valid " + VALID_DAYS + " days)");

            TaskResult result = new TaskResult(task);
            result.setStatus(TaskResult.Status.COMPLETED);
            result.addOutputData("certPem", pem);
            result.addOutputData("serialNumber", serial.toString());
            result.addOutputData("algorithm", "SHA256withRSA");
            result.addOutputData("keySize", KEY_SIZE);
            result.addOutputData("validDays", VALID_DAYS);
            result.addOutputData("domain", domain);
            return result;

        } catch (Exception e) {
            System.err.println("  [generate] Certificate generation failed: " + e.getMessage());
            TaskResult fail = new TaskResult(task);
            fail.setStatus(TaskResult.Status.FAILED);
            fail.setReasonForIncompletion("Certificate generation failed: " + e.getMessage());
            return fail;
        }
    }

    /**
     * Builds a self-signed X.509v3 certificate in DER format using raw ASN.1 encoding.
     * No JDK-internal or third-party crypto libraries needed.
     */
    private byte[] buildSelfSignedCert(String cn, BigInteger serial, Date notBefore, Date notAfter, KeyPair keyPair) throws Exception {
        // TBSCertificate fields
        byte[] serialBytes = derInteger(serial);
        byte[] signatureAlgo = derSequence(concat(
                derOid(new int[]{1, 2, 840, 113549, 1, 1, 11}), // sha256WithRSAEncryption
                derNull()
        ));
        byte[] issuer = derSequence(derSet(derSequence(concat(
                derOid(new int[]{2, 5, 4, 3}), // CN OID
                derUtf8String(cn)
        ))));
        byte[] validity = derSequence(concat(derUtcTime(notBefore), derUtcTime(notAfter)));
        byte[] subject = issuer; // self-signed
        byte[] subjectPubKeyInfo = keyPair.getPublic().getEncoded(); // Already DER-encoded
        byte[] version = derExplicit(0, derInteger(BigInteger.valueOf(2))); // v3

        byte[] tbsCert = derSequence(concat(
                version, serialBytes, signatureAlgo, issuer, validity, subject, subjectPubKeyInfo
        ));

        // Sign the TBSCertificate
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(keyPair.getPrivate());
        sig.update(tbsCert);
        byte[] signature = sig.sign();

        // Certificate = SEQUENCE { tbsCert, signatureAlgorithm, signatureValue }
        return derSequence(concat(
                tbsCert,
                signatureAlgo,
                derBitString(signature)
        ));
    }

    // --- ASN.1 DER encoding helpers ---

    private byte[] derTag(int tag, byte[] content) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(tag);
        writeLength(out, content.length);
        out.writeBytes(content);
        return out.toByteArray();
    }

    private byte[] derSequence(byte[] content) { return derTag(0x30, content); }
    private byte[] derSet(byte[] content) { return derTag(0x31, content); }
    private byte[] derBitString(byte[] data) {
        byte[] content = new byte[data.length + 1];
        content[0] = 0; // no unused bits
        System.arraycopy(data, 0, content, 1, data.length);
        return derTag(0x03, content);
    }
    private byte[] derNull() { return new byte[]{0x05, 0x00}; }

    private byte[] derInteger(BigInteger val) {
        byte[] bytes = val.toByteArray();
        return derTag(0x02, bytes);
    }

    private byte[] derUtf8String(String s) { return derTag(0x0C, s.getBytes()); }

    private byte[] derUtcTime(Date date) {
        @SuppressWarnings("deprecation")
        String utc = String.format("%02d%02d%02d%02d%02d%02dZ",
                date.getYear() % 100, date.getMonth() + 1, date.getDate(),
                date.getHours(), date.getMinutes(), date.getSeconds());
        return derTag(0x17, utc.getBytes());
    }

    private byte[] derOid(int[] oid) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(40 * oid[0] + oid[1]);
        for (int i = 2; i < oid.length; i++) {
            writeBase128(out, oid[i]);
        }
        return derTag(0x06, out.toByteArray());
    }

    private byte[] derExplicit(int tagNum, byte[] content) {
        return derTag(0xA0 | tagNum, content);
    }

    private void writeLength(ByteArrayOutputStream out, int length) {
        if (length < 128) {
            out.write(length);
        } else if (length < 256) {
            out.write(0x81);
            out.write(length);
        } else {
            out.write(0x82);
            out.write(length >> 8);
            out.write(length & 0xFF);
        }
    }

    private void writeBase128(ByteArrayOutputStream out, int value) {
        if (value < 128) {
            out.write(value);
        } else {
            int[] stack = new int[5];
            int pos = 0;
            while (value > 0) {
                stack[pos++] = value & 0x7F;
                value >>= 7;
            }
            for (int i = pos - 1; i >= 0; i--) {
                out.write(stack[i] | (i > 0 ? 0x80 : 0));
            }
        }
    }

    private byte[] concat(byte[]... arrays) {
        int len = 0;
        for (byte[] a : arrays) len += a.length;
        byte[] result = new byte[len];
        int pos = 0;
        for (byte[] a : arrays) {
            System.arraycopy(a, 0, result, pos, a.length);
            pos += a.length;
        }
        return result;
    }
}
