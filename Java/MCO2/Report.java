/********************
 * Report.java
 * Contract for every report. A new report is one new class that implements
 * this interface and is added to the list in ReportService.
 ********************/

import java.util.List;

public interface Report
{
    /** A finished table: header names plus rows already formatted for output. */
    record Table(List<String> headers, List<List<String>> rows) { }

    String title();      // e.g. "Banking Privilege Tiers"

    String fileName();   // e.g. "report1_banking_privilege_tiers.csv"

    Table generate(Bank bank);

    /** Short description shown under the report title (used by the custom reports). */
    default String note()
    {
        return "";
    }
}
