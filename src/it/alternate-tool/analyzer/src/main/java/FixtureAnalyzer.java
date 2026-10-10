import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class FixtureAnalyzer {
  // Contract stub only: exercises the adapter, not Kotlin parsing or schema validation.
  public static void main(String[] args) throws Exception {
    var options = new HashMap<String, String>();
    for (int i = 0; i < args.length; i += 2) {
      options.put(args[i], args[i + 1]);
    }
    Path root = Path.of(options.get("--project"));
    if (!Files.isRegularFile(root.resolve("Example.kt")) || !options.containsKey("--classpath")) {
      throw new IllegalArgumentException("Missing source or compile classpath");
    }
    Files.writeString(
        Path.of(options.get("--output")), "[{\"FullName\":\"sample.KotlinExample\"}]");
  }
}
