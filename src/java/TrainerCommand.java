import java.io.IOException;

interface TrainerCommand {
    int run(CsvStore store, String id) throws IOException;
}
