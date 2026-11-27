/********************
 * ActivityRankingReport.java
 * REQ-0009 - Report 4C (custom): Customer Activity Ranking.
 *
 * Justification: the busiest customers drive transaction costs and deserve
 * the most service attention. Ranking every account by total money moved
 * (deposits + withdrawals) shows who they are and how large their typical
 * transaction is.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ActivityRankingReport implements Report
{
    private record Row(String name, int count, BigDecimal volume) { }

    @Override
    public String title()
    {
        return "Report 4C - Customer Activity Ranking";
    }

    @Override
    public String fileName()
    {
        return "report4c_customer_activity_ranking.csv";
    }

    @Override
    public String note()
    {
        return "Accounts ranked by total amount moved (deposits + withdrawals).";
    }

    @Override
    public Table generate(Bank bank)
    {
        List<Row> rows = new ArrayList<>();
        for (Account account : bank.getAccounts())
        {
            BigDecimal volume = BigDecimal.ZERO;
            for (Transaction t : account.getHistory()) volume = volume.add(t.amount());
            rows.add(new Row(account.getName(), account.getHistory().size(), volume));
        }
        rows.sort(Comparator.comparing(Row::volume).reversed().thenComparing(Row::name));

        List<List<String>> out = new ArrayList<>();
        int rank = 1;
        for (Row r : rows)
        {
            BigDecimal average = r.volume().divide(BigDecimal.valueOf(r.count()), 2, RoundingMode.HALF_UP);
            out.add(List.of(String.valueOf(rank++), r.name(), String.valueOf(r.count()),
                    Formats.money(r.volume()), Formats.money(average)));
        }
        return new Table(List.of("Rank", "AccountName", "TransactionCount", "TotalVolume", "AverageTransaction"), out);
    }
}
