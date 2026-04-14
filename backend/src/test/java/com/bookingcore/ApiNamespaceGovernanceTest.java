package com.bookingcore;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class ApiNamespaceGovernanceTest {

  @Test
  void sourceCodeMustNotIntroduceApiAdminNamespace() throws IOException {
    List<String> violations = new ArrayList<>();
    collectViolations(Path.of("..", "backend", "src", "main", "java"), violations);
    collectViolations(Path.of("..", "frontend", "src"), violations);

    assertThat(violations)
        .as("`/api/admin` is forbidden; use `/api/system` for system-admin APIs.")
        .isEmpty();
  }

  private void collectViolations(Path root, List<String> violations) throws IOException {
    if (!Files.exists(root)) {
      return;
    }
    try (Stream<Path> stream = Files.walk(root)) {
      stream
          .filter(Files::isRegularFile)
          .filter(this::isSourceFile)
          .forEach(
              file -> {
                try {
                  String content = Files.readString(file);
                  if (content.contains("/api/admin")) {
                    violations.add(file.normalize().toString());
                  }
                } catch (IOException ex) {
                  throw new RuntimeException(ex);
                }
              });
    }
  }

  private boolean isSourceFile(Path path) {
    String filename = path.getFileName().toString();
    return filename.endsWith(".java")
        || filename.endsWith(".ts")
        || filename.endsWith(".tsx")
        || filename.endsWith(".js")
        || filename.endsWith(".jsx");
  }
}
