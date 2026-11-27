/********************
 * MonthlyCashFlowReport.java
 * REQ-0009 - Report 4B (custom): Monthly Cash Flow.
 *
 * Justification: management needs to know whether money is flowing into or
 * out of the bank over time. One row per month with total deposits,
 * withdrawals and the net flow shows liquidity trends at a glance.
 ********************/

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MonthlyCashFlowReport implements Report
{
    private static class MonthTotals
    {
        BigDecimal deposits = BigDecimal.ZERO;
        BigDecimal withdrawals = BigDecimal.ZERO;
        int transactions = 0;
    }

    @Override
    public String title()
    {
        return "Report 4B - Monthly Cash Flow";
    }

    @Override
    public String fileName()
    {
        return "report4b_monthly_cash_flow.csv";
    }

    @Override
    public String note()
    {
        return "Total deposits, withdrawals and net flow per month (oldest first).";
    }

    @Override
    public Table generate(Bank bank)
    {
        Map<YearMonth, MonthTotals> months = new TreeMap<>();
        bank.transactions().forEach(t ->
        {
            MonthTotals m = months.computeIfAbsent(YearMonth.from(t.date()), k -> new MonthTotals());
            m.transactions++;
            if (t.isDeposit()) m.deposits = m.deposits.add(t.amount());
            else m.withdrawals = m.withdrawals.add(t.amount());
        });

        List<List<String>> out = new ArrayList<>();
        months.forEach((month, m) -> out.add(List.of(
                month.format(DateTimeFormatter.ofPattern("MM/uuuu")),
                String.valueOf(m.transactions), Formats.money(m.deposits), Formats.money(m.withdrawals),
                Formats.money(m.deposits.subtract(m.withdrawals)))));
        return new Table(List.of("Month", "Transactions", "TotalDeposits", "TotalWithdrawals", "NetCashFlow"), out);
    }
}
