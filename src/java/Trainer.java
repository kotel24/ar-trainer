import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class Trainer {
    public static void main(String[] args) throws IOException {
        Map<String, TrainerCommand> commands = Map.of("demo", new DemoCommand(), "check", new CheckCommand());
        if (args.length != 3 || !commands.containsKey(args[0])) {
            System.out.println("Использование:");
            System.out.println("  java -cp out Trainer demo <каталог данных> <номер сценария>");
            System.out.println("  java -cp out Trainer check <каталог данных> <номер попытки>");
            return;
        }
        int status = commands.get(args[0]).run(new CsvStore(Path.of(args[1])), args[2]);
        if (status != 0) {
            System.exit(status);
        }
    }
}
