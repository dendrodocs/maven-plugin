package dev.dendrodocs.maven;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import java.io.File;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import javax.inject.Inject;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/** Generates a DendroDocs intermediary JSON file for the current Maven module. */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES,
        requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateMojo extends AbstractMojo {
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Inject
    private RepositorySystem repositorySystem;

    @Parameter(defaultValue = "${repositorySystemSession}", readonly = true, required = true)
    private RepositorySystemSession repositorySession;

    /** Optional replacement for Maven's registered main source roots. */
    @Parameter
    private List<File> sourceDirectories;

    /** Destination for the merged JSON document. */
    @Parameter(defaultValue = "${project.build.directory}/dendrodocs.json", property = "dendrodocs.outputFile", required = true)
    private File outputFile;

    /** Whether to indent the JSON output. */
    @Parameter(defaultValue = "true", property = "dendrodocs.pretty")
    private boolean pretty;

    /** Whether to skip generation for this module. */
    @Parameter(defaultValue = "false", property = "dendrodocs.skip")
    private boolean skip;

    /** Maximum execution time per source root. */
    @Parameter(defaultValue = "120", property = "dendrodocs.timeoutSeconds")
    private int timeoutSeconds;

    /** Creates the plugin goal. Maven supplies its configuration. */
    public GenerateMojo() {}

    @Override
    public void execute() throws MojoExecutionException {
        if (skip || "pom".equals(project.getPackaging())) {
            getLog().info("Skipping DendroDocs for " + project.getArtifactId());
            return;
        }
        if (timeoutSeconds <= 0) {
            throw new MojoExecutionException("dendrodocs.timeoutSeconds must be positive");
        }
        Path workspace = null;
        try {
            List<Path> roots = sourceRoots();
            if (roots.isEmpty()) {
                getLog().info("No main source directories for " + project.getArtifactId());
                return;
            }
            Path destination = absolute(outputFile.toPath());
            Files.createDirectories(destination.getParent());
            workspace = Files.createTempDirectory(destination.getParent(), ".dendrodocs-");
            List<String> classpath = classpath(workspace);
            ObjectMapper mapper = new ObjectMapper().enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
            ArrayNode merged = mapper.createArrayNode();
            Set<String> typeNames = new HashSet<>();
            int index = 0;
            for (Path root : roots) {
                Set<String> expectedTypes = validateSyntax(root);
                Path output = workspace.resolve("root-" + index++ + ".json");
                analyzeRoot(root, output, classpath);
                if (!Files.isRegularFile(output)) {
                    throw new IOException("Analyzer did not create fresh JSON for " + root);
                }
                JsonNode result = mapper.readTree(output.toFile());
                if (result == null || !result.isArray()) {
                    throw new IOException("Analyzer output must be a JSON array for " + root);
                }
                Set<String> rootTypes = new HashSet<>();
                for (JsonNode type : result) {
                    JsonNode name = type.get("FullName");
                    if (name == null || !name.isTextual() || name.asText().isBlank()) {
                        throw new IOException("Analyzer produced a type without a FullName for " + root);
                    }
                    if (!typeNames.add(name.asText())) {
                        throw new IOException("Duplicate type across source roots: " + name.asText());
                    }
                    rootTypes.add(name.asText());
                    merged.add(type);
                }
                expectedTypes.removeAll(rootTypes);
                if (!expectedTypes.isEmpty()) {
                    throw new IOException("Analyzer omitted top-level types from " + root + ": " + expectedTypes);
                }
            }
            Path staged = workspace.resolve("merged.json");
            var writer = pretty ? mapper.writerWithDefaultPrettyPrinter() : mapper.writer();
            writer.writeValue(staged.toFile(), merged);
            try {
                Files.move(staged, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(staged, destination, StandardCopyOption.REPLACE_EXISTING);
            }
            getLog().info("DendroDocs wrote " + merged.size() + " types to " + destination);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new MojoExecutionException("DendroDocs analysis was interrupted", exception);
        } catch (IOException | DependencyResolutionRequiredException | RuntimeException exception) {
            throw new MojoExecutionException("DendroDocs generation failed: " + exception.getMessage(), exception);
        } finally {
            if (workspace != null) {
                cleanup(workspace);
            }
        }
    }

    // Package visibility keeps the orchestration independently testable without a process mock framework.
    void analyzeRoot(Path root, Path output, List<String> classpath) throws IOException, InterruptedException {
        var configuration = new java.util.Properties();
        try (var resource = GenerateMojo.class.getResourceAsStream("/dendrodocs.properties")) {
            if (resource == null) {
                throw new IOException("Missing embedded analyzer version");
            }
            configuration.load(resource);
        }
        var request = new ArtifactRequest()
                .setArtifact(new DefaultArtifact("dev.dendrodocs:tool:jar:" + configuration.getProperty("tool.version")))
                .setRepositories(project.getRemotePluginRepositories());
        try {
            Path toolJar = repositorySystem.resolveArtifact(repositorySession, request).getArtifact().getFile().toPath();
            AnalyzerProcess.run(toolJar, root, output, classpath, timeoutSeconds);
        } catch (ArtifactResolutionException exception) {
            throw new IOException("Cannot resolve the DendroDocs analyzer; check plugin repositories and offline cache", exception);
        }
    }

    private Path absolute(Path path) {
        return (path.isAbsolute() ? path : project.getBasedir().toPath().resolve(path)).toAbsolutePath().normalize();
    }

    private Set<String> validateSyntax(Path root) throws IOException {
        var compiler = javax.tools.ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IOException("DendroDocs requires a JDK with the Java compiler, not a JRE");
        }
        List<Path> sources;
        try (var files = Files.walk(root)) {
            sources = files.filter(Files::isRegularFile).filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
        if (sources.isEmpty()) {
            return new HashSet<>();
        }
        var diagnostics = new javax.tools.DiagnosticCollector<javax.tools.JavaFileObject>();
        try (var manager = compiler.getStandardFileManager(diagnostics, java.util.Locale.ROOT, java.nio.charset.StandardCharsets.UTF_8)) {
            var units = manager.getJavaFileObjectsFromPaths(sources);
            var task = (com.sun.source.util.JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "21"), null, units);
            // Parse only: no annotation processing, attribution, bytecode generation, or execution.
            Set<String> expectedTypes = new HashSet<>();
            for (var unit : task.parse()) {
                String packageName = unit.getPackageName() == null ? "" : unit.getPackageName() + ".";
                for (var declaration : unit.getTypeDecls()) {
                    if (declaration instanceof com.sun.source.tree.ClassTree type) {
                        expectedTypes.add(packageName + type.getSimpleName());
                    }
                }
            }
            var errors = diagnostics.getDiagnostics().stream()
                    .filter(d -> d.getKind() == javax.tools.Diagnostic.Kind.ERROR).limit(5).toList();
            if (!errors.isEmpty()) {
                throw new IOException("Invalid Java source in " + root + ": " + errors);
            }
            return expectedTypes;
        }
    }

    private List<Path> sourceRoots() throws IOException {
        boolean explicit = sourceDirectories != null;
        List<Path> candidates = explicit
                ? sourceDirectories.stream().map(File::toPath).toList()
                : project.getCompileSourceRoots().stream().map(Path::of).toList();
        List<Path> roots = new ArrayList<>();
        for (Path candidate : candidates) {
            Path root = absolute(candidate);
            if (!Files.exists(root) && !explicit) {
                continue;
            }
            if (!Files.isDirectory(root)) {
                throw new IOException("Source directory does not exist or is not a directory: " + root);
            }
            root = root.toRealPath();
            if (!roots.contains(root)) {
                roots.add(root);
            }
        }
        // A parent source root already includes its descendants; avoid analyzing the same files twice.
        return roots.stream().filter(root -> roots.stream().noneMatch(other -> !other.equals(root) && root.startsWith(other)))
                .sorted().toList();
    }

    private List<String> classpath(Path workspace) throws IOException, DependencyResolutionRequiredException {
        List<String> result = new ArrayList<>();
        for (String element : project.getCompileClasspathElements()) {
            Path path = absolute(Path.of(element));
            if (Files.isRegularFile(path) && path.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".jar")) {
                result.add(path.toString());
            } else if (Files.isDirectory(path)) {
                // The analyzer accepts JARs only. Package compiled reactor classes without copying resources.
                Path jar = workspace.resolve("classes-" + result.size() + ".jar");
                try (var output = new JarOutputStream(Files.newOutputStream(jar)); var files = Files.walk(path)) {
                    for (Path file : files.filter(Files::isRegularFile).filter(p -> !Files.isSymbolicLink(p))
                            .filter(p -> p.toString().endsWith(".class")).sorted().toList()) {
                        JarEntry entry = new JarEntry(path.relativize(file).toString().replace('\\', '/'));
                        entry.setTime(0);
                        output.putNextEntry(entry);
                        Files.copy(file, output);
                        output.closeEntry();
                    }
                }
                result.add(jar.toString());
            }
        }
        return List.copyOf(result);
    }

    private void cleanup(Path workspace) {
        try (var files = Files.walk(workspace)) {
            for (Path file : files.sorted(Comparator.reverseOrder()).toList()) {
                Files.delete(file);
            }
        } catch (IOException exception) {
            getLog().warn("Could not remove DendroDocs temporary files: " + exception.getMessage());
        }
    }
}
