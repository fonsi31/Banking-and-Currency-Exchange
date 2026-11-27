/********************
 * Formats.java
 * Shared date, time, number and CSV helpers.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Formats
{
    // Strict resolver rejects impossible dates such as 31/02/2026.
    public static final DateTimeFormatter DATE_IN = DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter TIME_IN = DateTimeFormatter.ofPattern("H:mm:ss").withResolverStyle(ResolverStyle.STRICT);
    public static final DateTimeFormatter DATE_OUT = DateTimeFormatter.ofPattern("dd/MM/uuuu");
    public static final DateTimeFormatter TIME_OUT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private Formats() { }

    /** 1234.5 -> "1234.50" (console, JSON and CSV storage). */
    public static String plain(BigDecimal value)
    {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    /** 1234.5 -> "1,234.50" (report CSVs, REQ-0012). */
    public static String money(BigDecimal value)
    {
        return String.format(Locale.US, "%,.2f", value);
    }

    // CSV

    /** Wraps a field in quotes when it contains a comma, quote or line break. */
    public static String csvField(String field)
    {
        if (field.contains(",") || field.contains("\"") || field.contains("\n"))
        {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    public static String csvLine(List<String> fields)
    {
        StringBuilder line = new StringBuilder();
        for (int i = 0; i < fields.size(); i++)
        {
            if (i > 0) line.append(',');
            line.append(csvField(fields.get(i)));
        }
        return line.toString();
    }

    /** Splits one CSV line, honouring quoted fields. */
    public static List<String> parseCsvLine(String line)
    {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;

        for (int i = 0; i < line.length(); i++)
        {
            char c = line.charAt(i);
            if (quoted)
            {
                if (c == '"' && i + 1 < line.length() && line.charAt(i + 1) == '"')
                {
                    current.append('"');
                    i++;
                }
                else if (c == '"')
                {
                    quoted = false;
                }
                else
                {
                    current.append(c);
                }
            }
            else if (c == '"')
            {
                quoted = true;
            }
            else if (c == ',')
            {
                fields.add(current.toString());
                current.setLength(0);
            }
            else
            {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
