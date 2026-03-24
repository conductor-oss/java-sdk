# Automated TLS Certificate Rotation: From SSLSocket Inspection to JKS Deployment

It is 3 AM and your monitoring alerts fire: the TLS certificate on `api.production.com` expires in 6 days. Someone needs to check all domains, generate replacements, deploy them, and verify the handshake -- without a typo that takes down production. This is exactly the kind of multi-step, failure-prone operational task that should never be done manually.

This example implements a full certificate rotation pipeline that performs real TLS handshakes via `SSLSocket`, generates cryptographically valid X.509 certificates using raw ASN.1 DER encoding, deploys them to a Java KeyStore, and verifies the result with a second TLS handshake.

## Pipeline Flow

```
cr_discover   (SSLSocket handshake -> extract cert expiry, subject, issuer)
     |
     v
cr_generate   (RSA 2048-bit keypair -> self-signed X.509v3 via raw DER)
     |
     v
cr_deploy     (parse PEM -> store in JKS keystore on disk)
     |
     v
cr_verify     (second SSLSocket handshake -> confirm TLS protocol + cipher suite)
```

Each step passes its full output as the next step's input via Conductor's `${previous_ref.output}` wiring.

## Worker: DiscoverWorker (`cr_discover`)

Opens a real TLS connection to the target domain using a trust-all `X509TrustManager` -- this allows inspecting certificates even when they are self-signed or already expired:

```java
SSLContext ctx = SSLContext.getInstance("TLS");
ctx.init(null, trustAll, new java.security.SecureRandom());
try (SSLSocket socket = (SSLSocket) factory.createSocket(domain, port)) {
    socket.setSoTimeout(10_000);
    socket.startHandshake();
    X509Certificate cert = (X509Certificate) socket.getSession().getPeerCertificates()[0];
    long daysRemaining = ChronoUnit.DAYS.between(Instant.now(), cert.getNotAfter().toInstant());
}
```

The `expiringSoon` flag triggers at fewer than 30 days remaining. Expired certificates produce negative `daysRemaining` values.

### Error Classification

The worker distinguishes three failure modes with different retry semantics:

| Exception | Status | Rationale |
|---|---|---|
| `UnknownHostException` | `FAILED_WITH_TERMINAL_ERROR` | DNS is wrong -- retrying will not help |
| `ConnectException` / `SocketTimeoutException` | `FAILED` (retryable) | Network blip -- Conductor can retry |
| Other TLS exceptions | `FAILED` (retryable) | Transient handshake failures |

## Worker: GenerateWorker (`cr_generate`)

Generates a real 2048-bit RSA keypair and builds a self-signed X.509v3 certificate entirely from scratch using raw ASN.1 DER encoding -- no BouncyCastle, no JDK-internal `sun.security` classes.

The DER encoding is built from primitives: `derSequence()`, `derInteger()`, `derOid()`, `derUtcTime()`, `derBitString()`. The OID `1.2.840.113549.1.1.11` identifies SHA256withRSA. The TBSCertificate is assembled, signed with the private key, and wrapped in a final SEQUENCE:

```java
byte[] tbsCert = derSequence(concat(
    version, serialBytes, signatureAlgo, issuer, validity, subject, subjectPubKeyInfo));
Signature sig = Signature.getInstance("SHA256withRSA");
sig.initSign(keyPair.getPrivate());
sig.update(tbsCert);
byte[] signature = sig.sign();
```

Serial numbers are derived from `System.currentTimeMillis()`, ensuring uniqueness across invocations. The certificate is valid for 365 days. Output is PEM-encoded with MIME base64 at 64-character line width.

## Worker: DeployWorker (`cr_deploy`)

Parses the PEM certificate using `CertificateFactory.getInstance("X.509")`, creates an empty JKS keystore, and stores the certificate under alias `{domain-with-dots-replaced}-cert`:

```java
KeyStore ks = KeyStore.getInstance("JKS");
ks.load(null, null);
ks.setCertificateEntry(alias, cert);
```

The keystore is written to a temp file with password `"changeit"`. In production, this would target a load balancer, reverse proxy, or cloud certificate manager.

## Worker: VerifyWorker (`cr_verify`)

Performs a second TLS handshake to confirm the domain is still reachable and reports the negotiated TLS protocol version and cipher suite. Uses the same trust-all approach as DiscoverWorker and the same three-tier error classification.

## Test Coverage

5 test classes, 17 tests:

**DiscoverWorkerTest (7 tests):** Real TLS handshake to `google.com` (network-gated via `assumeTrue`), expiry threshold accuracy check, expired cert detection against `expired.badssl.com`, missing/blank domain rejection, terminal failure on unresolvable domain.

**GenerateWorkerTest (4 tests):** Valid PEM output with BEGIN/END markers, SHA256withRSA algorithm, 2048-bit key size, 365-day validity. Missing/blank domain rejection.

**DeployWorkerTest (3 tests):** Real keystore file creation on disk, alias naming convention verification, failure when certPem is missing.

**CertRotationIntegrationTest (3 tests):** Generate-to-Deploy data contract (PEM produced by generate is parseable by deploy), Deploy fails without Generate output, different domains produce different serial numbers.

**VerifyWorkerTest (3 tests):** Real TLS handshake verification (network-gated), protocol and cipher suite output, missing domain rejection.

---

## Production Notes

See [PRODUCTION.md](PRODUCTION.md) for deployment guidance, monitoring expectations, and security considerations.

---

> **How to run this example:** See [RUNNING.md](../../RUNNING.md) for prerequisites, build commands, Docker setup, and CLI usage.
