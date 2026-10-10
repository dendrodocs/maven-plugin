package dev.dendrodocs.maven;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/** Runs the standalone analyzer without loading its bundled libraries into Maven. */
final class AnalyzerProcess {
  private AnalyzerProcess() {}

  static void run(Path jar, Path sources, Path output, List<String> classpath, int timeoutSeconds)
      throws IOException, InterruptedException {
    Path javaExecutable =
        Path.of(
            System.getProperty("java.home"),
            "bin",
            System.getProperty("os.name").startsWith("Windows") ? "java.exe" : "java");
    List<String> command =
        new ArrayList<>(
            List.of(
                javaExecutable.toString(),
                "-jar",
                jar.toString(),
                "--project",
                sources.toString(),
                "--output",
                output.toString()));
    if (!classpath.isEmpty()) {
      command.add("--classpath");
      command.add(String.join(java.io.File.pathSeparator, classpath));
    }
    Path log = output.resolveSibling(output.getFileName() + ".log");
    Process process =
        new ProcessBuilder(command).redirectErrorStream(true).redirectOutput(log.toFile()).start();
    try {
      if (!process.waitFor(timeoutSeconds, TimeUnit.SECONDS)) {
        throw new IOException("Analyzer exceeded " + timeoutSeconds + " seconds for " + sources);
      }
      if (process.exitValue() != 0) {
        throw new IOException(
            "Analyzer exited with code "
                + process.exitValue()
                + " for "
                + sources
                + ": "
                + diagnostic(log));
      }
      if (!Files.isRegularFile(output)) {
        throw new IOException(
            "Analyzer did not create JSON for "
                + sources
                + "; schema validation may have failed. "
                + diagnostic(log));
      }
    } finally {
      if (process.isAlive()) {
        process.destroyForcibly();
        process.waitFor(5, TimeUnit.SECONDS);
      }
    }
  }

  private static String diagnostic(Path log) throws IOException {
    try (var reader = Files.newBufferedReader(log)) {
      char[] buffer = new char[4096];
      int count = reader.read(buffer);
      return count < 0 ? "No diagnostics." : new String(buffer, 0, count);
    }
  }
}
