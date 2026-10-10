package dev.dendrodocs.maven;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarFile;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GenerateMojoTest {
    @TempDir Path directory;

    private final List<Path> visited = new ArrayList<>();
    private final List<String> classpathEntries = new ArrayList<>();

    private MavenProject project(String packaging) {
        Model model = new Model();
        model.setArtifactId("sample");
        model.setPackaging(packaging);
        Build build = new Build();
        build.setDirectory(directory.resolve("target").toString());
        build.setOutputDirectory(directory.resolve("target/classes").toString());
        model.setBuild(build);
        MavenProject project = new MavenProject(model);
        project.setFile(directory.resolve("pom.xml").toFile());
        return project;
    }

    private GenerateMojo mojo(MavenProject project, RootAction action) throws Exception {
        GenerateMojo mojo = new GenerateMojo() {
            @Override void analyzeRoot(Path root, Path output, List<String> classpath)
                    throws IOException, InterruptedException {
                visited.add(root);
                classpathEntries.addAll(classpath);
                action.run(root, output, classpath);
            }
        };
        set(mojo, "project", project);
        set(mojo, "outputFile", directory.resolve("target/dendrodocs.json").toFile());
        set(mojo, "pretty", true);
        set(mojo, "timeoutSeconds", 120);
        return mojo;
    }

    private Path source(MavenProject project, String name) throws IOException {
        Path root = Files.createDirectories(directory.resolve(name));
        project.addCompileSourceRoot(root.toString());
        return root.toRealPath();
    }

    private static void set(GenerateMojo mojo, String name, Object value) throws Exception {
        Field field = GenerateMojo.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(mojo, value);
    }

    private void assertClean() throws IOException {
        try (var files = Files.list(directory.resolve("target"))) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".dendrodocs-")));
        }
    }

    @Test void mergesSourceRootsAndCreatesOutputParents() throws Exception {
        MavenProject project = project("jar");
        source(project, "b");
        source(project, "a");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output,
                "[{\"FullName\":\"sample." + root.getFileName() + "\"}]"));
        mojo.execute();
        String result = Files.readString(directory.resolve("target/dendrodocs.json"));
        assertTrue(result.indexOf("sample.a") < result.indexOf("sample.b"));
        assertEquals(2, visited.size());
        assertClean();
    }

    @Test void removesRepeatedAndNestedSourceRoots() throws Exception {
        MavenProject project = project("jar");
        Path parent = source(project, "src");
        project.addCompileSourceRoot(parent.toString());
        source(project, "src/nested");
        mojo(project, (root, output, cp) -> Files.writeString(output, "[]")).execute();
        assertEquals(List.of(parent), visited);
    }

    @Test void rejectsMissingFreshOutputAndPreservesPreviousResult() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        Path previous = directory.resolve("target/dendrodocs.json");
        Files.createDirectories(previous.getParent());
        Files.writeString(previous, "previous result");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> {});
        assertThrows(MojoExecutionException.class, mojo::execute);
        assertEquals("previous result", Files.readString(previous));
        assertClean();
    }

    @Test void rejectsInvalidJson() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "not JSON"));
        assertThrows(MojoExecutionException.class, mojo::execute);
        assertFalse(Files.exists(directory.resolve("target/dendrodocs.json")));
        assertClean();
    }

    @Test void rejectsTrailingJsonTokens() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "[] []"));
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test void rejectsNonArrayOutput() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "{}"));
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test void rejectsDuplicateTypes() throws Exception {
        MavenProject project = project("jar");
        source(project, "one");
        source(project, "two");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "[{\"FullName\":\"sample.Duplicate\"}]"));
        assertTrue(assertThrows(MojoExecutionException.class, mojo::execute).getMessage().contains("Duplicate type"));
    }

    @Test void rejectsTypesWithoutNames() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "[{}]"));
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test void supportsRelativeSourceAndOutputPathsAndCompactJson() throws Exception {
        MavenProject project = project("jar");
        Files.createDirectories(directory.resolve("source with spaces"));
        GenerateMojo mojo = mojo(project, (root, output, cp) -> Files.writeString(output, "[{\"FullName\":\"sample.Type\"}]"));
        set(mojo, "sourceDirectories", List.of(new File("source with spaces")));
        set(mojo, "outputFile", new File("custom output/result.json"));
        set(mojo, "pretty", false);
        mojo.execute();
        assertEquals("[{\"FullName\":\"sample.Type\"}]", Files.readString(directory.resolve("custom output/result.json")));
    }

    @Test void failsForMissingExplicitSourceDirectory() throws Exception {
        GenerateMojo mojo = mojo(project("jar"), (root, output, cp) -> fail("Unexpected analysis"));
        set(mojo, "sourceDirectories", List.of(new File("missing")));
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test void skipsMissingDefaultSourceDirectory() throws Exception {
        MavenProject project = project("jar");
        project.addCompileSourceRoot(directory.resolve("missing").toString());
        mojo(project, (root, output, cp) -> fail("Unexpected analysis")).execute();
        assertFalse(Files.exists(directory.resolve("target")));
    }

    @Test void skipsPomModules() throws Exception {
        MavenProject project = project("pom");
        source(project, "src");
        mojo(project, (root, output, cp) -> fail("Unexpected analysis")).execute();
        assertFalse(Files.exists(directory.resolve("target")));
    }

    @Test void honorsSkip() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> fail("Unexpected analysis"));
        set(mojo, "skip", true);
        mojo.execute();
        assertFalse(Files.exists(directory.resolve("target")));
    }

    @Test void snapshotsCompiledClassesWithoutResources() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        Path classes = Files.createDirectories(directory.resolve("target/classes/sample"));
        Files.write(classes.resolve("Type.class"), new byte[] {1, 2, 3});
        Files.writeString(classes.resolve("secret.properties"), "excluded resource");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> {
            assertEquals(1, cp.size());
            try (JarFile jar = new JarFile(cp.getFirst())) {
                assertNotNull(jar.getEntry("sample/Type.class"));
                assertNull(jar.getEntry("sample/secret.properties"));
            }
            Files.writeString(output, "[]");
        });
        mojo.execute();
        assertFalse(Files.exists(Path.of(classpathEntries.getFirst())));
        assertClean();
    }

    @Test void validatesTimeout() throws Exception {
        GenerateMojo mojo = mojo(project("jar"), (root, output, cp) -> fail("Unexpected analysis"));
        set(mojo, "timeoutSeconds", 0);
        assertThrows(MojoExecutionException.class, mojo::execute);
    }

    @Test void rejectsMalformedJavaBeforeInvokingAnalyzer() throws Exception {
        MavenProject project = project("jar");
        Path root = source(project, "src");
        Files.writeString(root.resolve("Invalid.java"), "public class Invalid { NOT JAVA }");
        GenerateMojo mojo = mojo(project, (source, output, cp) -> fail("Invalid input reached the analyzer"));
        assertTrue(assertThrows(MojoExecutionException.class, mojo::execute).getMessage().contains("Invalid Java source"));
        assertClean();
    }

    @Test void rejectsAnalyzerOmissionOfValidTopLevelTypes() throws Exception {
        MavenProject project = project("jar");
        Path root = source(project, "src");
        Files.writeString(root.resolve("Actual.java"), "package sample; public class Actual {}");
        GenerateMojo mojo = mojo(project, (source, output, cp) -> Files.writeString(output, "[]"));
        assertTrue(assertThrows(MojoExecutionException.class, mojo::execute).getMessage().contains("sample.Actual"));
        assertFalse(Files.exists(directory.resolve("target/dendrodocs.json")));
        assertClean();
    }

    @Test void preservesInterruptionAndCleansTemporaryFiles() throws Exception {
        MavenProject project = project("jar");
        source(project, "src");
        GenerateMojo mojo = mojo(project, (root, output, cp) -> { throw new InterruptedException(); });
        try {
            assertThrows(MojoExecutionException.class, mojo::execute);
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
        assertClean();
    }

    @FunctionalInterface interface RootAction {
        void run(Path root, Path output, List<String> classpath) throws IOException, InterruptedException;
    }
}
