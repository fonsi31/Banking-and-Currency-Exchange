/********************
 * TransactionFile.java
 * Handles bank_transactions.csv (REQ-0001 to REQ-0005):
 * reads the file, keeps only the PHP rows, checks and cleans each row,
 * and adds new transactions at the end.
 ********************/

import java.io.BufferedReader;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

public class TransactionFile
{
    private static final List<String> HEADER = List.of(
            "account_name", "account_number", "date", "time", "currency", "amount", "type", "current_balance");
    private static final int NAME = 0, NUMBER = 1, DATE = 2, TIME = 3, CURRENCY = 4, AMOUNT = 5, TYPE = 6, BALANCE = 7;
    private static final Pattern ACCOUNT_NUMBER = Pattern.compile("\\d{10}");

    /** What one load gives back: counts for the log, the cleaned rows, and any problems found. */
    public record LoadResult(int rowsRead, int phpRows, int nonPhpRows, List<Transaction> transactions, List<String> errors) { }

    private final Path path;

    public TransactionFile(Path path)
    {
        this.path = path;
    }

    public Path getPath()
    {
        return path;
    }

    // Reading

    public LoadResult load() throws IOException
    {
        List<Transaction> valid = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        int rowsRead = 0;
        int phpRows = 0;
        int nonPhpRows = 0;

        if (Files.exists(path))
        {
            try (BufferedReader in = Files.newBufferedReader(path, StandardCharsets.UTF_8))
            {
                in.readLine(); // skip the header
                int lineNo = 1;
                String line;
                while ((line = in.readLine()) != null)
                {
                    lineNo++;
                    if (line.isBlank()) continue;
                    rowsRead++;

                    List<String> f = Formats.parseCsvLine(line);
                    try
                    {
                        if (f.size() != HEADER.size())
                        {
                            throw new IllegalArgumentException("expected " + HEADER.size() + " columns but found " + f.size());
                        }
                        requireValue(f, CURRENCY);
                        if (!f.get(CURRENCY).trim().equalsIgnoreCase("PHP")) // REQ-0003: PHP only, skip the rest
                        {
                            nonPhpRows++;
                            continue;
                        }
                        phpRows++;
                        valid.add(parseRow(f));
                    }
                    catch (IllegalArgumentException | DateTimeException e)
                    {
                        errors.add("Line " + lineNo + ": " + e.getMessage());
                    }
                }
            }
        }
        valid.sort(Comparator.comparing(Transaction::timestamp)); // stable sort, so same-timestamp rows keep their file order
        return new LoadResult(rowsRead, phpRows, nonPhpRows, valid, errors);
    }

    private static Transaction parseRow(List<String> f)
    {
        for (int column : new int[] {NAME, NUMBER, DATE, TIME, AMOUNT, TYPE, BALANCE}) requireValue(f, column);

        String number = f.get(NUMBER).trim();
        if (!ACCOUNT_NUMBER.matcher(number).matches())
        {
            throw new IllegalArgumentException("invalid account number '" + number + "'");
        }

        LocalDate date;
        LocalTime time;
        try
        {
            date = LocalDate.parse(f.get(DATE).trim(), Formats.DATE_IN);
        }
        catch (DateTimeParseException e)
        {
            throw new IllegalArgumentException("invalid date '" + f.get(DATE) + "'");
        }
        try
        {
            time = LocalTime.parse(f.get(TIME).trim(), Formats.TIME_IN);
        }
        catch (DateTimeParseException e)
        {
            throw new IllegalArgumentException("invalid time '" + f.get(TIME) + "'");
        }

        BigDecimal amount = parseMoney(f.get(AMOUNT), "amount");
        BigDecimal balance = parseMoney(f.get(BALANCE), "current_balance");
        if (amount.signum() <= 0)
        {
            throw new IllegalArgumentException("amount must be greater than zero ('" + f.get(AMOUNT) + "')");
        }

        return new Transaction(f.get(NAME).trim(), number, LocalDateTime.of(date, time),
                Transaction.Type.fromLabel(f.get(TYPE)), amount, balance);
    }

    private static void requireValue(List<String> f, int column)
    {
        if (f.get(column).isBlank())
        {
            throw new IllegalArgumentException("missing value for " + HEADER.get(column));
        }
    }

    private static BigDecimal parseMoney(String text, String column)
    {
        try
        {
            return new BigDecimal(text.trim().replace(",", ""));
        }
        catch (NumberFormatException e)
        {
            throw new IllegalArgumentException("invalid " + column + " '" + text + "'");
        }
    }

    // Writing

    /** Adds one transaction row at the end. Makes the file (with its header) first if it's not there yet. */
    public void append(Transaction t) throws IOException
    {
        boolean isNew = !Files.exists(path) || Files.size(path) == 0;
        if (isNew)
        {
            Files.writeString(path, Formats.csvLine(HEADER) + System.lineSeparator(), StandardOpenOption.CREATE);
        }
        else if (!endsWithNewline())
        {
            Files.writeString(path, System.lineSeparator(), StandardOpenOption.APPEND);
        }

        List<String> row = List.of(
                t.accountName(), t.accountNumber(),
                t.timestamp().format(Formats.DATE_OUT), t.timestamp().format(Formats.TIME_OUT),
                "PHP", Formats.plain(t.amount()), t.type().label(), Formats.plain(t.balanceAfter()));
        Files.writeString(path, Formats.csvLine(row) + System.lineSeparator(), StandardOpenOption.APPEND);
    }

    private boolean endsWithNewline() throws IOException
    {
        try (RandomAccessFile file = new RandomAccessFile(path.toFile(), "r"))
        {
            file.seek(file.length() - 1);
            int last = file.read();
            return last == '\n' || last == '\r';
        }
    }
}
