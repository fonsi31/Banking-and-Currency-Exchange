/********************
 * ReportService.java
 * Runs every report, saves each one to its own CSV file (REQ-0012), prints a
 * 2-row preview, and writes summary.json (REQ-0010).
 ********************/

import java.io.IOException;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ReportService
{
    private static final int PREVIEW_ROWS = 2;

    private final Path outputFolder;
    private final InterestProjectionReport interestReport = new InterestProjectionReport();
    private final List<Report> reports = List.of(
            new PrivilegeTierReport(),
            interestReport,
            new SuspiciousBehaviorReport(),
            new DormantAccountsReport());    // Report 4: our custom report

    public ReportService(Path outputFolder)
    {
        this.outputFolder = outputFolder;
    }

    public void generateAll(Bank bank, PrintStream out) throws IOException
    {
        int number = 1;
        for (Report report : reports)
        {
            Report.Table table = report.generate(bank);
            writeCsv(report.fileName(), table);

            out.println("Report " + number + ": " + report.title());
            if (!report.note().isEmpty()) out.println(report.note());
            printPreview(table, out);
            out.println("(Full table exported to " + report.fileName() + ")");
            out.println();
            number++;
        }

        String summary = buildSummary(bank);
        Files.writeString(outputFolder.resolve("summary.json"), summary + System.lineSeparator(), StandardCharsets.UTF_8);
        out.println("Summary State (summary.json):");
        out.println(summary);
    }

    // CSV export

    private void writeCsv(String fileName, Report.Table table) throws IOException
    {
        List<String> lines = new ArrayList<>(table.rows().size() + 1);
        lines.add(Formats.csvLine(table.headers()));
        for (List<String> row : table.rows()) lines.add(Formats.csvLine(row));
        Files.write(outputFolder.resolve(fileName), lines, StandardCharsets.UTF_8);
    }

    // Console preview

    private static void printPreview(Report.Table table, PrintStream out)
    {
        int columns = table.headers().size();
        int shown = Math.min(PREVIEW_ROWS, table.rows().size());

        int[] width = new int[columns];
        for (int c = 0; c < columns; c++)
        {
            width[c] = table.headers().get(c).length();
            for (int r = 0; r < shown; r++) width[c] = Math.max(width[c], table.rows().get(r).get(c).length());
        }

        printRow(table.headers(), width, out);
        for (int r = 0; r < shown; r++) printRow(table.rows().get(r), width, out);
        if (table.rows().isEmpty()) out.println("(no rows)");
        out.println();
    }

    private static void printRow(List<String> cells, int[] width, PrintStream out)
    {
        StringBuilder line = new StringBuilder();
        for (int c = 0; c < cells.size(); c++)
        {
            boolean last = c == cells.size() - 1;
            line.append(last ? cells.get(c) : String.format("%-" + width[c] + "s | ", cells.get(c)));
        }
        out.println(line);
    }

    // summary.json

    private String buildSummary(Bank bank)
    {
        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        for (Transaction t : (Iterable<Transaction>) bank.transactions()::iterator)
        {
            if (t.isDeposit()) deposits = deposits.add(t.amount());
            else withdrawals = withdrawals.add(t.amount());
        }

        BigDecimal balance = BigDecimal.ZERO;
        for (Account account : bank.getAccounts()) balance = balance.add(account.getBalance());

        BigDecimal interest = BigDecimal.ZERO;
        for (InterestProjectionReport.Entry e : interestReport.entries(bank)) interest = interest.add(e.expectedInterest());

        return "{\n"
                + "  \"total_accounts\": " + bank.getAccounts().size() + ",\n"
                + "  \"total_deposits\": " + Formats.plain(deposits) + ",\n"
                + "  \"total_withdrawals\": " + Formats.plain(withdrawals) + ",\n"
                + "  \"total_balance\": " + Formats.plain(balance) + ",\n"
                + "  \"total_interest_expense\": " + Formats.plain(interest) + "\n"
                + "}";
    }
}
