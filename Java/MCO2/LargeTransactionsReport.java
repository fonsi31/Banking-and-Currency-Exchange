/********************
 * LargeTransactionsReport.java
 * REQ-0009 - Report 4D (custom): Large Transactions.
 *
 * Justification: single transactions of Php 250,000.00 or more are what an
 * auditor reviews first (anti-money-laundering checks). Report 3 only looks
 * at daily totals, so this report lists the individual transactions, newest
 * first.
 ********************/

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LargeTransactionsReport implements Report
{
    private static final BigDecimal THRESHOLD = new BigDecimal("250000.00");

    @Override
    public String title()
    {
        return "Report 4D - Large Transactions";
    }

    @Override
    public String fileName()
    {
        return "report4d_large_transactions.csv";
    }

    @Override
    public String note()
    {
        return "Single transactions of Php 250,000.00 or more, newest first.";
    }

    @Override
    public Table generate(Bank bank)
    {
        List<Transaction> large = new ArrayList<>();
        bank.transactions().filter(t -> t.amount().compareTo(THRESHOLD) >= 0).forEach(large::add);
        large.sort(Comparator.comparing(Transaction::timestamp).reversed().thenComparing(Transaction::accountName));

        List<List<String>> out = new ArrayList<>();
        for (Transaction t : large)
        {
            out.add(List.of(t.accountName(), t.date().format(Formats.DATE_OUT), t.timestamp().format(Formats.TIME_OUT),
                    t.type().label(), Formats.money(t.amount())));
        }
        return new Table(List.of("AccountName", "Date", "Time", "TransactionType", "Amount"), out);
    }
}
