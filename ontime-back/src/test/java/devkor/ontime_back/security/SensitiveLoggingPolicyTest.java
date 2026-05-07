package devkor.ontime_back.security;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SensitiveLoggingPolicyTest {

    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");
    private static final Pattern LOG_STATEMENT = Pattern.compile(
            "log\\.(?:trace|debug|info|warn|error)\\s*\\((.*?)\\);",
            Pattern.DOTALL
    );
    private static final List<String> SENSITIVE_LOG_TERMS = List.of(
            "authorization",
            "firebaseToken",
            "password",
            "secret",
            "token"
    );

    @Test
    void logStatementsDoNotReferenceSensitiveKeyNames() throws IOException {
        try (Stream<Path> sourceFiles = Files.walk(MAIN_SOURCE_ROOT)) {
            List<String> violations = sourceFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .flatMap(this::findSensitiveLogStatements)
                    .toList();

            assertThat(violations)
                    .as("Log statements must not reference sensitive key names or sensitive variables")
                    .isEmpty();
        }
    }

    @Test
    void requestLoggingAspectDoesNotReadRequestBodies() throws IOException {
        String loggingAspect = Files.readString(MAIN_SOURCE_ROOT.resolve("devkor/ontime_back/LoggingAspect.java"));

        assertThat(loggingAspect)
                .doesNotContain("org.springframework.web.bind.annotation.RequestBody")
                .doesNotContain("requestBody")
                .doesNotContain(".toString()");
    }

    @Test
    void dtoPackageDoesNotGenerateStringRepresentations() throws IOException {
        try (Stream<Path> dtoFiles = Files.walk(MAIN_SOURCE_ROOT.resolve("devkor/ontime_back/dto"))) {
            List<String> violations = dtoFiles
                    .filter(path -> path.toString().endsWith(".java"))
                    .filter(path -> {
                        try {
                            String source = Files.readString(path);
                            return source.contains("@ToString") || source.contains("String toString()");
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .map(Path::toString)
                    .toList();

            assertThat(violations)
                    .as("DTOs should not auto-render sensitive request payloads")
                    .isEmpty();
        }
    }

    private Stream<String> findSensitiveLogStatements(Path sourceFile) {
        try {
            String source = Files.readString(sourceFile);
            Matcher matcher = LOG_STATEMENT.matcher(source);
            Stream.Builder<String> violations = Stream.builder();

            while (matcher.find()) {
                String statement = matcher.group(1);
                String normalizedStatement = statement.toLowerCase(Locale.ROOT);
                SENSITIVE_LOG_TERMS.stream()
                        .filter(term -> normalizedStatement.contains(term.toLowerCase(Locale.ROOT)))
                        .findFirst()
                        .ifPresent(term -> violations.add(sourceFile + " logs sensitive term: " + term));
            }

            return violations.build();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
