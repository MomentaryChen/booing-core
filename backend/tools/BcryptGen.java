import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Small local helper to print bcrypt hashes using the same encoder as the Spring Boot app.
 *
 * <p>Run from {@code backend/}:
 *
 * <pre>
 * mvn -q dependency:build-classpath -Dmdep.includeScope=compile -Dmdep.outputFile=target/cp.txt
 * javac -cp "@target/cp.txt" tools/BcryptGen.java
 * java -cp "@target/cp.txt;tools" BcryptGen DemoMerchant123! DemoClient123!
 * </pre>
 */
public final class BcryptGen {
  public static void main(String[] args) {
    if (args.length == 0) {
      System.err.println("usage: BcryptGen <password> [<password> ...]");
      System.exit(2);
    }
    BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    for (String raw : args) {
      System.out.println(encoder.encode(raw));
    }
  }
}
