/********************
 * InterestProjectionReport.java
 * REQ-0007 - Report 2: Interest Expense Projection.
 * Expected interest over the next 30 days for accounts with at least
 * Php 10,000.00, sorted by ExpectedInterest (highest first).
 ********************/

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InterestProjectionReport implements Report
{
    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("10000.00");
    private static final int PROJECTION_DAYS = 30;

    public record Entry(Account account, BigDecimal expectedInterest) { }

    @Override
    public String title()
    {
        return "Interest Expense Projection";
    }

    @Override
    public String fileName()
    {
        return "report2_interest_expense_projection.csv";
    }

    /** The summary uses this too, so the total interest expense always matches this report. */
    public List<Entry> entries(Bank bank)
    {
        List<Entry> entries = new ArrayList<>();
        for (Account account : bank.getAccounts())
        {
            if (account.getBalance().compareTo(MINIMUM_BALANCE) >= 0)
            {
                entries.add(new Entry(account, account.expectedInterest(PROJECTION_DAYS)));
            }
        }
        entries.sort(Comparator.comparing(Entry::expectedInterest).reversed()
                .thenComparing(e -> e.account().getName()));
        return entries;
    }

    @Override
    public Table generate(Bank bank)
    {
        List<List<String>> rows = new ArrayList<>();
        for (Entry e : entries(bank))
        {
            rows.add(List.of(e.account().getName(), Formats.money(e.account().getBalance()), Formats.money(e.expectedInterest())));
        }
        return new Table(List.of("AccountName", "CurrentBalance", "ExpectedInterest"), rows);
    }
}
