import java.io.BufferedWriter;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;


class Printer {
    static void print(String[] a, String[] b, boolean[] deleted, boolean[] inserted,
                      boolean highlight) throws IOException {
    
        Writer out = new BufferedWriter(new OutputStreamWriter(
                new FileOutputStream(FileDescriptor.out), StandardCharsets.ISO_8859_1), 1 << 16);

        int i = 0;
        int j = 0;
        while (i < a.length || j < b.length) {
       
            if (i < a.length && j < b.length && !deleted[i] && !inserted[j]) {
                out.write(" " + a[i] + "\n");
                i++;
                j++;
                continue;
            }

            int delStart = i;
            while (i < a.length && deleted[i]) i++;
            int insStart = j;
            while (j < b.length && inserted[j]) j++;

            for (int p = delStart; p < i; p++) {
                out.write("-" + a[p] + "\n");
            }
            for (int q = insStart; q < j; q++) {
                out.write("+" + b[q] + "\n");

                int p = delStart + (q - insStart);
                if (highlight && p < i) {
                    out.write(highlightLine(a[p], b[q]) + "\n");
                }
            }
        }
        out.flush();
    }


    static String highlightLine(String oldLine, String newLine) {
        int[] oldChars = codePoints(oldLine);
        int[] newChars = codePoints(newLine);
        Myers diff = new Myers(oldChars, newChars);
        return "? " + ranges(diff.deleted) + " | " + ranges(diff.inserted);
    }

    static int[] codePoints(String line) {
        byte[] bytes = line.getBytes(StandardCharsets.ISO_8859_1);
        return new String(bytes, StandardCharsets.UTF_8).codePoints().toArray();
    }


    static String ranges(boolean[] changed) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < changed.length) {
            if (!changed[i]) {
                i++;
                continue;
            }
            int start = i;
            while (i < changed.length && changed[i]) i++;
            if (sb.length() > 0) sb.append(',');
            sb.append(start).append('-').append(i);
        }
        return sb.length() == 0 ? "." : sb.toString();
    }
}
