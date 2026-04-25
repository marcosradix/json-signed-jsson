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

## 🧠 How does the algorithm work in practice? (Under the Hood)

For Canonicalization to be infallible, the engine transforms any JSON object into a *Deterministic String* (clean and ordered) before calculating its hash and applying the signature. The secret in the Java ecosystem lies in the strict configuration of the `ObjectMapper` (Jackson) and the use of Spring Interceptors.

### 1. The Canonicalization Core (`JssonC`)
The main class configures Jackson to alphabetically order any key and minify the data output, eradicating discrepancies arising from "pretty prints".

```java
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import java.util.TreeMap;

public class JssonC {
    private static final ObjectMapper canonicalMapper;

    static {
        canonicalMapper = new ObjectMapper();
        // Forces alphabetical sorting of keys and properties
        canonicalMapper.configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true);
        canonicalMapper.configure(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY, true);
        // Removes spaces, tabs, and line breaks (mandatory minification)
        canonicalMapper.configure(SerializationFeature.INDENT_OUTPUT, false);
    }

    public static String canonicalize(Object input) throws Exception {
        // Converts to a TreeMap to force natural alphabetical consistency and recursion
        Object sortedObject = canonicalMapper.convertValue(input, TreeMap.class);
        return canonicalMapper.writeValueAsString(sortedObject);
    }
}
```

### 2. Spring Automation Magic (`JssonResponseInterceptor`)
To validate perfectly across APIs and services, the `JssonResponseInterceptor` acts as an invisible middleware. It intercepts the response originated by the API, applies the canonicalization engine to it, generates the cryptographic signature, and returns the formatted JSSON without you having to modify the controller-level code.

```java
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;
// (other imports...)

@ControllerAdvice
public class JssonResponseInterceptor implements ResponseBodyAdvice<Object> {

    @Override
    public boolean supports(MethodParameter returnType, Class converterType) {
        // Intercepts only resources marked with the annotation
        return returnType.hasMethodAnnotation(JssonSign.class); 
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType contentType,
                                  Class converterType, ServerHttpRequest req, ServerHttpResponse res) {
        try {
            // 1. Generates the rigorous Canonical String via JSSON-C source code
            String canonicalData = JssonC.canonicalize(body);

            // 2. Performs the cryptographic signature of this pure data using private key (Ed25519)
            String signature = CryptoService.sign(canonicalData, privateKey); 

            // 3. Assembles and alters the original response by coupling the proof/security branch ($jsson)
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode finalNode = mapper.valueToTree(body);
            ObjectNode proofNode = finalNode.putObject("$jsson");
            proofNode.put("v", "1");
            proofNode.put("alg", "Ed25519");
            proofNode.put("sig", signature);

            return finalNode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate confidential JSSON signatures", e);
        }
    }
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

#OpenSource #DataIntegrity #JSSON #JavaSpring #CyberSecurity
