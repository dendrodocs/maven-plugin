package dev.dendrodocs.maven;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import javax.tools.ToolProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AnalyzerProcessTest {
    @TempDir Path directory;

    private Path analyzer(String statements) throws IOException {
        Path source = directory.resolve("FakeAnalyzer.java");
        Files.writeString(source, "public class FakeAnalyzer { public static void main(String[] args) throws Exception { " + statements + " } }");
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", directory.toString(), source.toString()));
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
        manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, "FakeAnalyzer");
        Path jar = directory.resolve("analyzer with spaces.jar");
        try (var output = new JarOutputStream(Files.newOutputStream(jar), manifest)) {
            output.putNextEntry(new JarEntry("FakeAnalyzer.class"));
            Files.copy(directory.resolve("FakeAnalyzer.class"), output);
            output.closeEntry();
        }
        return jar;
    }

    @Test void reportsNonZeroExitWithDiagnostics() throws Exception {
        Path jar = analyzer("System.out.println(\"useful diagnostic\"); System.exit(3);");
        IOException error = assertThrows(IOException.class,
                () -> AnalyzerProcess.run(jar, directory, directory.resolve("output.json"), List.of(), 10));
        assertTrue(error.getMessage().contains("code 3"));
        assertTrue(error.getMessage().contains("useful diagnostic"));
    }

    @Test void rejectsSuccessWithoutOutput() throws Exception {
        Path jar = analyzer("System.out.println(\"schema validation failed\");");
        IOException error = assertThrows(IOException.class,
                () -> AnalyzerProcess.run(jar, directory, directory.resolve("output.json"), List.of(), 10));
        assertTrue(error.getMessage().contains("did not create JSON"));
        assertTrue(error.getMessage().contains("schema validation failed"));
    }

    @Test void terminatesTimedOutProcess() throws Exception {
        Path marker = directory.resolve("process.pid");
        Path jar = analyzer("java.nio.file.Files.writeString(java.nio.file.Path.of(args[1]).resolve(\"process.pid\"), Long.toString(ProcessHandle.current().pid())); Thread.sleep(10000);");
        long start = System.nanoTime();
        IOException error = assertThrows(IOException.class,
                () -> AnalyzerProcess.run(jar, directory, directory.resolve("output.json"), List.of(), 1));
        assertTrue(error.getMessage().contains("exceeded 1 seconds"));
        assertTrue((System.nanoTime() - start) / 1_000_000_000.0 < 5);
        if (Files.exists(marker)) {
            long processId = Long.parseLong(Files.readString(marker));
            assertFalse(ProcessHandle.of(processId).map(ProcessHandle::isAlive).orElse(false));
        }
    }
}
