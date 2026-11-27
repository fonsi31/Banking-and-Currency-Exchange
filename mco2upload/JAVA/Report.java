/********************
 * Report.java
 * The contract every report follows. Want a new report? Just make a class
 * that implements this and add it to the list in ReportService.
 ********************/

import java.util.List;

public interface Report
{
    /** A ready-to-print table: header names plus rows that are already formatted. */
    record Table(List<String> headers, List<List<String>> rows) { }

    String title();      // e.g. "Banking Privilege Tiers"

    String fileName();   // e.g. "report1_banking_privilege_tiers.csv"

    Table generate(Bank bank);

    /** Short blurb shown under the report title (only reports that need one override this). */
    default String note()
    {
        return "";
    }
}
