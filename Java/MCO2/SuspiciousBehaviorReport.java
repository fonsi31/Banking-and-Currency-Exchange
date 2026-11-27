/********************
 * SuspiciousBehaviorReport.java
 * REQ-0008 - Report 3: Suspicious Client Behavior.
 * One row per account per active day. Sorted by Date (oldest first), then
 * RiskFlagLevel (Suspicious goes before Normal).
 *
 * TurnoverVelocity = TotalWithdrawal / TotalDeposit x 100. If there were no
 * deposits that day the ratio doesn't exist, so we show 0.00 and only the
 * Php 500,000.00 withdrawal rule can flag that day.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SuspiciousBehaviorReport implements Report
{
    private static final BigDecimal WITHDRAWAL_LIMIT = new BigDecimal("500000.00");
    private static final BigDecimal VELOCITY_LIMIT = new BigDecimal("90");
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    /** Running totals for one account on one day. */
    private static class DayTotals
    {
        BigDecimal withdrawals = BigDecimal.ZERO;
        BigDecimal deposits = BigDecimal.ZERO;
        int depositCount = 0;

        void add(Transaction t)
        {
            if (t.isDeposit())
            {
                deposits = deposits.add(t.amount());
                depositCount++;
            }
            else
            {
                withdrawals = withdrawals.add(t.amount());
            }
        }

        boolean isSuspicious()
        {
            // Compare withdrawals x 100 with deposits x 90 so we skip division and rounding.
            boolean highVelocity = deposits.signum() > 0
                    && withdrawals.multiply(HUNDRED).compareTo(deposits.multiply(VELOCITY_LIMIT)) >= 0;
            return withdrawals.compareTo(WITHDRAWAL_LIMIT) >= 0 || highVelocity;
        }

        BigDecimal velocity()
        {
            if (deposits.signum() == 0) return BigDecimal.ZERO;
            return withdrawals.multiply(HUNDRED).divide(deposits, 2, RoundingMode.HALF_UP);
        }
    }

    private record Row(String name, LocalDate date, DayTotals totals, boolean suspicious) { }

    @Override
    public String title()
    {
        return "Suspicious Client Behavior";
    }

    @Override
    public String fileName()
    {
        return "report3_suspicious_client_behavior.csv";
    }

    @Override
    public Table generate(Bank bank)
    {
        List<Row> rows = new ArrayList<>();
        for (Account account : bank.getAccounts())
        {
            Map<LocalDate, DayTotals> days = new TreeMap<>();
            for (Transaction t : account.getHistory())
            {
                days.computeIfAbsent(t.date(), d -> new DayTotals()).add(t);
            }
            days.forEach((date, totals) -> rows.add(new Row(account.getName(), date, totals, totals.isSuspicious())));
        }

        // Date oldest first, Suspicious before Normal, then name so the output is always in the same order.
        rows.sort(Comparator.comparing(Row::date)
                .thenComparing(Comparator.comparing(Row::suspicious).reversed())
                .thenComparing(Row::name));

        List<List<String>> out = new ArrayList<>(rows.size());
        for (Row r : rows)
        {
            DayTotals t = r.totals();
            out.add(List.of(r.name(), r.date().format(Formats.DATE_OUT), Formats.money(t.withdrawals),
                    String.valueOf(t.depositCount), Formats.money(t.deposits), Formats.money(t.velocity()),
                    r.suspicious() ? "Suspicious" : "Normal"));
        }
        return new Table(List.of("AccountName", "Date", "TotalWithdrawalAmounts", "NumberOfDeposits",
                "TotalDepositAmounts", "TurnoverVelocity(%)", "RiskFlagLevel"), out);
    }
}
