/********************
 * PrivilegeTierReport.java
 * REQ-0006 - Report 1: Banking Privilege Tiers.
 * Clients whose AverageDailyBalance in the last month is at least Php 100,000.00,
 * sorted by AverageDailyBalance (descending).
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PrivilegeTierReport implements Report
{
    private static final BigDecimal MINIMUM_AVERAGE = new BigDecimal("100000.00");
    private static final BigDecimal GOLD_FROM = new BigDecimal("500000.00");
    private static final BigDecimal PLATINUM_FROM = new BigDecimal("1000000.00");

    private record Row(Account account, long age, BigDecimal averageDailyBalance, String tier) { }

    @Override
    public String title()
    {
        return "Banking Privilege Tiers";
    }

    @Override
    public String fileName()
    {
        return "report1_banking_privilege_tiers.csv";
    }

    @Override
    public Table generate(Bank bank)
    {
        List<Row> rows = new ArrayList<>();
        LocalDate asOf = bank.latestActivityDate();

        if (asOf != null)
        {
            YearMonth lastMonth = YearMonth.from(asOf); // the latest month in the data
            for (Account account : bank.getAccounts())
            {
                BigDecimal average = averageDailyBalance(account, lastMonth);
                if (average.compareTo(MINIMUM_AVERAGE) >= 0)
                {
                    rows.add(new Row(account, bank.daysSinceOpened(account, asOf), average, tierOf(average)));
                }
            }
        }
        rows.sort(Comparator.comparing(Row::averageDailyBalance).reversed()
                .thenComparing(r -> r.account().getName()));

        List<List<String>> out = new ArrayList<>();
        for (Row r : rows)
        {
            out.add(List.of(r.account().getName(), String.valueOf(r.age()), Formats.money(r.account().getBalance()),
                    Formats.money(r.averageDailyBalance()), r.tier()));
        }
        return new Table(List.of("AccountName", "AccountAge", "CurrentBalance", "AverageDailyBalance", "PrivilegeTier"), out);
    }

    /** Sum of every day's End-of-Day balance in the month / number of days in the month. */
    private static BigDecimal averageDailyBalance(Account account, YearMonth month)
    {
        BigDecimal sum = BigDecimal.ZERO;
        for (int day = 1; day <= month.lengthOfMonth(); day++)
        {
            sum = sum.add(account.endOfDayBalance(month.atDay(day)));
        }
        return sum.divide(BigDecimal.valueOf(month.lengthOfMonth()), 10, RoundingMode.HALF_UP);
    }

    private static String tierOf(BigDecimal average)
    {
        if (average.compareTo(GOLD_FROM) < 0) return "Silver";
        if (average.compareTo(PLATINUM_FROM) < 0) return "Gold";
        return "Platinum";
    }
}
