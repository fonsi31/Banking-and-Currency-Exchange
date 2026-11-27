/********************
 * DormantAccountsReport.java
 * REQ-0009 - Report 4 (custom): Dormant Accounts.
 *
 * Why this report: a bank quietly loses deposits when customers stop using
 * their accounts. Listing accounts with no transaction in the last 7 days
 * (longest idle first) shows relationship managers who to reach out to
 * before the money leaves.
 ********************/

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DormantAccountsReport implements Report
{
    private static final int DORMANT_AFTER_DAYS = 7;

    private record Row(Account account, LocalDate lastDate, long daysIdle) { }

    @Override
    public String title()
    {
        return "Dormant Accounts";
    }

    @Override
    public String fileName()
    {
        return "report4_dormant_accounts.csv";
    }

    @Override
    public String note()
    {
        return "Accounts with no transaction in the last " + DORMANT_AFTER_DAYS + " days.";
    }

    @Override
    public Table generate(Bank bank)
    {
        List<Row> rows = new ArrayList<>();
        LocalDate asOf = bank.latestActivityDate();

        for (Account account : bank.getAccounts())
        {
            LocalDate last = account.lastActivity().toLocalDate();
            long idle = ChronoUnit.DAYS.between(last, asOf);
            if (idle >= DORMANT_AFTER_DAYS)
            {
                rows.add(new Row(account, last, idle));
            }
        }
        rows.sort(Comparator.comparingLong(Row::daysIdle).reversed()
                .thenComparing((Row r) -> r.account().getBalance(), Comparator.reverseOrder())
                .thenComparing(r -> r.account().getName()));

        List<List<String>> out = new ArrayList<>();
        for (Row r : rows)
        {
            BigDecimal balance = r.account().getBalance();
            out.add(List.of(r.account().getName(), r.account().getNumber(), r.lastDate().format(Formats.DATE_OUT),
                    String.valueOf(r.daysIdle()), Formats.money(balance)));
        }
        return new Table(List.of("AccountName", "AccountNumber", "LastTransactionDate", "DaysInactive", "CurrentBalance"), out);
    }
}
