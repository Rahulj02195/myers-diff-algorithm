import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        boolean known = args.length == 3 && (args[0].equals("lines") || args[0].equals("highlight"));
        if (!known) {
            System.err.println("usage: Main lines|highlight A_PATH B_PATH");
            System.exit(2);
        }
        String command = args[0];
        String aPath = args[1];
        String bPath = args[2];

        String[] a = readLines(aPath);
        String[] b = readLines(bPath);
        if (a == null || b == null) {
            System.exit(2);
        }

        // Give every distinct line a number, so the diff compares ints instead of strings.
        Map<String, Integer> ids = new HashMap<>();
        int[] aIds = toIds(a, ids);
        int[] bIds = toIds(b, ids);

        Myers diff = new Myers(aIds, bIds);

        try {
            Printer.print(a, b, diff.deleted, diff.inserted, command.equals("highlight"));
        } catch (IOException e) {
            System.err.println("error writing output: " + e.getMessage());
            System.exit(1);
        }
    }

    
    static String[] readLines(String path) {
        byte[] bytes;
        try {
            bytes = Files.readAllBytes(Path.of(path));
        } catch (Exception e) {
            System.err.println("cannot read " + path + ": " + e.getMessage());
            return null;
        }
        String text = new String(bytes, StandardCharsets.ISO_8859_1);
        String[] parts = text.split("\n", -1);
        // A final newline (or an empty file) leaves an empty last piece: drop it.
        int count = parts[parts.length - 1].isEmpty() ? parts.length - 1 : parts.length;
        String[] lines = new String[count];
        System.arraycopy(parts, 0, lines, 0, count);
        return lines;
    }

    static int[] toIds(String[] lines, Map<String, Integer> ids) {
        int[] result = new int[lines.length];
        for (int i = 0; i < lines.length; i++) {
            Integer id = ids.get(lines[i]);
            if (id == null) {
                id = ids.size();
                ids.put(lines[i], id);
            }
            result[i] = id;
        }
        return result;
    }
}
