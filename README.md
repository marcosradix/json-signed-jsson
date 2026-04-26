# JSSON: JavaScript Signed Object Notation 🛡️

The new trust primitive for the decentralized internet and mission-critical systems.

JSSON is a serialization and signature standard designed to ensure absolute data integrity. While standard JSON is merely a transport format, JSSON is a truly autonomous document, capable of proving its origin and integrity without relying on channel security (TLS) or persistent database queries.

## 🌟 What makes it disruptive?
- **Immutable Truth**: Data does not "trust" the network; it proves itself.
- **Rigorous Canonicalization (JSSON-C)**: A key-sorting and normalization algorithm that guarantees the Hash is identical across any programming language.
- **Zero-Lock-in**: Works anywhere (Java, Rust, Go, Python) without depending on a specific cloud provider.

## 🛠️ Technical Specification: JSSON-C
To ensure interoperability, the JSSON-C engine follows these strict rules:
1. **Ordering**: Object keys are sorted alphabetically.
2. **Minification**: No whitespaces or line breaks outside of strings.
3. **Numbers**: Standardized decimal representation (e.g., `10.0` → `10`).
4. **Arrays**: Original order is preserved, but internal elements are recursively canonicalized.

## 🛡️ Practical Example: Signed JSSON

JSSON secures data by injecting a cryptographic layer directly into the root of the object itself. The trusted issuer (such as a corporate CRM or an IoT device) signs the "Canonical String" of this object using its **Private Key**.

### The JSSON Structure

Here is an example of a perfectly signed payload with the resident signature (`$jsson`):

```json
{
  "A-key": "First",
  "details": {
    "id": 99,
    "service": "Unlimited 5G"
  },
  "apps_list": [
    "Netflix",
    "Spotify",
    "WhatsApp"
  ],
  "status": "active",
  "value": 29.9,
  "z-index": 1,
  "$jsson": {
    "v": "1",
    "alg": "Ed25519",
    "kid": "corp-core-key-001",
    "sig": "5f3e7a88...c1a21b3f"
  }
}
```

### Why does this mean the "End of Fraud"?

The inherent protection of JSSON renders bribery or interception during data transport ineffective.
If an attacker (man-in-the-middle or internal fraud in a legacy system) intercepts this JSON and maliciously removes a comma or changes the `"value"` from `29.9` to `9.9`, the following validation will occur at the destination:

1. The destination system temporarily removes or isolates the `$jsson` node to examine what will be proven.
2. The pending and tampered content passes back through the strict `JSSON-C` algorithm (smart cleaning and key reordering).
3. Then, the corresponding Hash of the processed and verified content is generated.
4. The newly obtained Hash will inevitably be **different** from the one intrinsically linked in `"sig"` (which was cryptographically originated via the legitimate issuer's private key).
5. The receiving system **brutally rejects** the transaction: The digital wax seal has been corrupted!

This eliminates costly data reconciliations between microservices; JSSON documentation does not trust the origin of the journey (TLS or IP transport), but solely validates what it is transporting — 100% *Offline-First*.

## 🎯 Selective Signing & Tokenized Signatures
JSSON supports **Selective Field Signing**, allowing you to protect critical fields (like `price` or `orderId`) while leaving others mutable. 

To keep the JSON root clean and secure, JSSON uses a **Tokenized Signature** format.

### The Signature Token (`sig`)
Instead of cluttering your JSON with metadata, the signing boundary (the list of included/excluded fields) is packed directly into the signature string using a **dot-separated token** format.

#### Token Anatomy
A JSSON signature string consists of two Base64Url segments:
`sig: [BOUNDARY_METADATA].[CRYPTOGRAPHIC_SIGNATURE]`

1.  **Segment 1: Boundary Metadata**
    *   **Value**: `fGluYzpbb3JkZXJJZCwgcHJpY2Vd`
    *   **Decoded**: `|inc:[orderId, price]`
    *   **Purpose**: Tells the verifier exactly which fields were hashed by the signer. This makes the payload **self-describing**.
2.  **Segment 2: Cryptographic Signature**
    *   **Value**: `MWdh714nVZe6TAhe91K91NDNC_6UmHRK_kbAm7FYeJlI8_j0pjtLNiHZouyGszMUQfbYPEWsXiN90mayirFQCA`
    *   **Algorithm**: Ed25519
    *   **Purpose**: Proves that both the data **and** the boundary metadata have not been tampered with.

> [!TIP]
> This structure is inspired by JWT (JSON Web Tokens) but optimized for raw JSON canonicalization instead of header/payload separation.

### Example: Tokenized Signed Payload
```json
{
  "orderId": "123",
  "service": "5G_PREMIUM",
  "price": 39.99,
  "$jsson": {
    "v": "1",
    "alg": "Ed25519",
    "sig": "fGluYzpbb3JkZXJJZCwgcHJpY2Vd.MWdh714nVZe6..."
  }
}
```
*(The `sig` field now acts as a self-describing cryptographic proof.)*

## 🍃 Integration with Java Spring Boot
JSSON was designed to be transparent for the developer through the `jsson-spring-boot-starter`.

### Key and Certificate Configuration (`application.yml`)
The library provides robust "Out-of-The-Box" configuration allowing security scaling. Natively adopt strict architectures through Authorized Certificates (JKS/PKCS12) or portable infrastructure files (PEM/Base64):

```yaml
jsson:
  enabled: true
  security:
    # [Option 1] Integration with Vaults and Corporate Certificates (Keystore PKCS12 / JKS)
    keystore-path: "classpath:corp-auth-cert.p12" 
    keystore-password: "changeit"
    key-alias: "jsson-edge-node"
    
    # [Option 2] DevOps & Container Secrets (OpenSSL format PEM / Base64)
    private-key-path: classpath:private-key.pem
    public-key-path: classpath:public-key.pem
```

### Usage in the Controller
Just add the annotation to transform a common JSON into a signed JSSON.

```java
@GetMapping("/client/{id}")
@JssonSign // The Interceptor handles the signature automatically
public ClientProfile getProfile(@PathVariable String id) {
    return service.getProfile(id);
}
```

### Automatic Validation
Upon data entry, Spring validates the cryptographic proof before your code is even executed.

```java
@PostMapping("/transaction")
public void execute(@RequestBody @JssonVerify Transaction tx) {
    // If the code reaches here, the data is 100% authentic.
}
```

### Testing the Demo App Locally
You can test the full cryptographic lifecycle using these `cURL` commands:

#### 1. Acquire a Tokenized Selective Payload
This endpoint excludes the `service` field from the signature but includes it in the response.
```bash
curl -s http://localhost:8080/api/orders/123 | jq .
```
**Response**:
```json
{
  "orderId": "123",
  "service": "5G_PREMIUM_UNLIMITED",
  "price": 39.99,
  "$jsson": {
    "v": "1",
    "alg": "Ed25519",
    "sig": "fGluYzphbGx8ZXhjOltzZXJ2aWNlXQ.cEfr0dgd..."
  }
}
```

#### 2. Submit Authentic Payload
The server decodes the boundary from the `sig` token and validates the data.
```bash
curl -X POST http://localhost:8080/api/orders/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "123",
    "service": "5G_PREMIUM_UNLIMITED",
    "price": 39.99,
    "$jsson": {
      "v": "1",
      "alg": "Ed25519",
      "sig": "fGluYzpbb3JkZXJJZCwgcHJpY2Vd.MWdh714..."
    }
  }'
```
**Status**: `200 OK`

#### 3. Submit Forged Payload (Blocked)
Changing the `price` from `39.99` to `1.99` results in a cryptographic violation.
```bash
curl -i -X POST http://localhost:8080/api/orders/process \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": "123",
    "service": "5G_PREMIUM_UNLIMITED",
    "price": 1.99,
    "$jsson": {
      "v": "1",
      "alg": "Ed25519",
      "sig": "fGluYzpbb3JkZXJJZCwgcHJpY2Vd.MWdh714..."
    }
  }'
```
**Status**: `403 Forbidden` (JSSON Fraud Lock triggered)

## 🧠 How does the algorithm work in practice? (Under the Hood)

For Canonicalization to be infallible, the engine transforms any JSON object into a *Deterministic String* (clean and ordered) before calculating its hash and applying the signature. The secret in the Java ecosystem lies in the strict configuration of the `ObjectMapper` (Jackson) and the use of Spring Interceptors.

### 1. The Canonicalization Core (`JssonC`)
The engine transforms any JSON object into a *Deterministic String* (clean and ordered) and then **appends the signing boundary** before calculating the final hash.

```java
public class JssonC {
    // ... setup canonicalMapper ...

    public static String canonicalize(Object input, Set<String> includes, Set<String> excludes) {
        // 1. Minify and Sort Keys
        String jsonResult = canonicalMapper.writeValueAsString(convertToSortedMap(input));
        
        // 2. Append Boundary Metadata (Boundary Binding)
        StringBuilder hashInput = new StringBuilder(jsonResult);
        if (includes != null && !includes.isEmpty()) {
            hashInput.append("|inc:").append(new TreeSet<>(includes));
        } else {
            hashInput.append("|inc:all");
        }
        
        return hashInput.toString(); // This is what gets signed
    }
}
```

### 2. Spring Automation Magic (`JssonResponseInterceptor`)
The interceptor automates the signing process, tokenizes the metadata, and injects the resident `$jsson` proof.

```java
public Object beforeBodyWrite(...) {
    // 1. Canonicalize with boundaries
    String canonicalData = JssonC.canonicalize(body, includes, excludes);

    // 2. Cryptographic Signature
    String rawSig = CryptoService.sign(canonicalData, privateKey); 

    // 3. Tokenize (Boundary + Signature)
    String boundary = JssonC.buildBoundaryString(includes, excludes);
    String token = Base64.encode(boundary) + "." + rawSig;

    // 4. Inject $jsson proof
    proofNode.put("sig", token);
    return finalNode;
}
```

## 🏗️ Project Structure (Monorepo)
- `/specs`: Formal definition of the JSSON-C algorithm.
- `/jsson-spring-boot-starter`: Core engine in Java and Spring Boot Starter.
- `/jsson-demo-app`: Use cases for Telecom (TMF-like), testing the Offline-First JSSON Engine.
- `/jsson-rust`: Ultra-performance implementation for Edge and IoT (Roadmap).

## 📈 Roadmap
- [x] JSSON-C v1 Specification.
- [x] Java/Jackson Canonicalization Engine.
- [x] Automatic Response Interceptor for Spring Boot.
- [ ] Rust SDK with WebAssembly (Wasm) bindings.
- [ ] CLI Validator: `jsson verify data.json`.

## 📝 License
Distributed under the Apache 2.0 license. Total freedom for the community and the industry.

## 💡 OpenAPI Generator
JSSON supports contract-first development! Check the `open-api-swagger-code-gen` branch to see how to integrate JSSON with OpenAPI Generator v7.4.0 using native `x-operation-extra-annotation` support.

#OpenSource #DataIntegrity #JSSON #JavaSpring #CyberSecurity
