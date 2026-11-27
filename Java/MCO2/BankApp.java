/********************
 * BankApp.java
 * The console screens. All the banking rules live in Bank, Account and the
 * report classes. This class just asks questions and prints answers.
 ********************/

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

public class BankApp
{
    private static final int MAX_DAYS = 10000; // max days allowed for the interest table
    private static final int MAX_ERRORS_SHOWN = 10;

    private final TransactionFile file = new TransactionFile(Path.of("bank_transactions.csv"));
    private final Bank bank = new Bank(file);
    private final ExchangeRates rates = new ExchangeRates();
    private final ReportService reports = new ReportService(Path.of("."));

    public void run()
    {
        loadDataset(false);

        while (true)
        {
            printSeparator();
            showMainMenu();

            int choice = Prompt.readInt("Choice: ", 1, 8);
            if (choice == 8) break; // Exit

            switch (choice)
            {
                case 1 -> registerAccountScreen();
                case 2 -> transactionScreen("Deposit", true);
                case 3 -> transactionScreen("Withdraw", false);
                case 4 -> currencyExchangeScreen();
                case 5 -> recordExchangeRateScreen();
                case 6 -> showInterestScreen();
                case 7 -> reportsScreen();
            }
            // every screen ends back here at the main menu
        }
        System.out.println("Thank you for banking with us.");
    }

    // Menus

    private static void printSeparator()
    {
        System.out.println();
        System.out.println("========================================");
    }

    private static void showMainMenu()
    {
        System.out.println("Select Transaction:");
        System.out.println("[1] Register Account");
        System.out.println("[2] Deposit Amount");
        System.out.println("[3] Withdraw Amount");
        System.out.println("[4] Currency Exchange");
        System.out.println("[5] Record Exchange Rates");
        System.out.println("[6] Show Interest Amount");
        System.out.println("[7] Generate Reports");
        System.out.println("[8] Exit");
        System.out.println();
    }

    private static void showCurrencyMenu()
    {
        for (ExchangeRates.Currency c : ExchangeRates.Currency.values())
        {
            System.out.println("[" + (c.ordinal() + 1) + "] " + c.getLabel());
        }
        System.out.println();
    }

    // [1] Register Account

    private void registerAccountScreen()
    {
        do
        {
            System.out.println("Register Account");
            try
            {
                String name = bank.checkName(Prompt.readText("Account Name (First name Last name): "));
                BigDecimal deposit = Prompt.readDecimalAtLeast(
                        "Initial Deposit (minimum " + Formats.plain(Bank.MINIMUM_INITIAL_DEPOSIT) + "): ", Bank.MINIMUM_INITIAL_DEPOSIT);

                Account account = bank.register(name, deposit);
                System.out.println("Account registered successfully.");
                showAccountSummary(account);
            }
            catch (IllegalArgumentException e)
            {
                System.out.println("Registration failed: " + e.getMessage());
            }
            catch (IOException e)
            {
                System.out.println("Could not save to " + file.getPath() + ": " + e.getMessage());
            }
            System.out.println();
        } while (Prompt.askYesNo("Register another account? (Y/N): ")); // N goes back to the main menu
    }

    // [2] Deposit and [3] Withdraw share one screen

    private void transactionScreen(String action, boolean isDeposit)
    {
        do
        {
            System.out.println(action + " Amount");
            Account account = selectAccount();
            if (account == null) return; // no account to work with, back to the main menu

            showAccountSummary(account);
            BigDecimal amount = readTransactionAmount(action, account);
            if (amount == null) return;

            try
            {
                if (isDeposit) bank.deposit(account, amount);
                else bank.withdraw(account, amount);
                System.out.println("Updated Balance: " + Formats.plain(account.getBalance()));
            }
            catch (IllegalArgumentException e) // bad amount or not enough balance
            {
                System.out.println(action + " failed: " + e.getMessage());
            }
            catch (IOException e)
            {
                System.out.println("Could not save to " + file.getPath() + ": " + e.getMessage());
            }
            System.out.println();
        } while (Prompt.askYesNo(action + " another amount? (Y/N): ")); // N goes back to the main menu
    }

    // [4] Currency Exchange

    private void currencyExchangeScreen()
    {
        do
        {
            System.out.println("Foreign Currency Exchange");
            System.out.println("Source Currency Option:");
            showCurrencyMenu();
            ExchangeRates.Currency source = Prompt.readCurrency("Source Currency: ");
            BigDecimal amount = Prompt.readPositiveDecimal("Source Amount: ");
            System.out.println();

            System.out.println("Exchanged Currency Options:");
            showCurrencyMenu();
            ExchangeRates.Currency target = Prompt.readCurrency("Exchange Currency: ");

            if (rates.canConvert(source, target))
            {
                System.out.println("Exchange Amount: " + Formats.plain(rates.convert(amount, source, target)));
            }
            else
            {
                System.out.println("Cannot convert currencies.");
            }
            System.out.println();
        } while (Prompt.askYesNo("Convert another currency? (Y/N): "));
    }

    // [5] Record Exchange Rates

    private void recordExchangeRateScreen()
    {
        System.out.println("Record Exchange Rate");
        showCurrencyMenu();

        ExchangeRates.Currency currency = Prompt.readCurrency("Select Foreign Currency: ");
        if (currency == ExchangeRates.BASE)
        {
            System.out.println(currency + " is the base currency; its rate is fixed at 1.00.");
            System.out.println();
            return;
        }
        rates.setRate(currency, Prompt.readPositiveDecimal("Exchange Rate: "));
        System.out.println();
    }

    // [6] Show Interest Amount

    private void showInterestScreen()
    {
        System.out.println("Show Interest Amount");
        Account account = selectAccount();
        if (account == null) return;

        showAccountSummary(account);
        System.out.println("Interest Rate: " + Account.ANNUAL_INTEREST_RATE
                .multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        System.out.println();

        int days = Prompt.readInt("Total Number of Days: ", 1, MAX_DAYS);
        System.out.println();

        StringBuilder table = new StringBuilder("Day  | Interest  | Balance    |\n"); // one print call so 10,000 rows still print fast
        for (Account.InterestEntry row : account.projectInterest(days))
        {
            table.append(String.format("%-4d | %-9s | %-10s |%n", row.getDay(),
                    Formats.plain(row.getInterest()), Formats.plain(row.getBalance())));
        }
        System.out.println(table);
    }

    // [7] Generate Reports

    private void reportsScreen()
    {
        boolean back = false;
        while (!back)
        {
            System.out.println();
            System.out.println("Generate Reports");
            System.out.println();
            System.out.println("[1] Load the file");
            System.out.println("[2] Generate Reports");
            System.out.println("[3] Back to the Main Menu");

            switch (Prompt.readInt("Enter choice: ", 1, 3))
            {
                case 1 -> loadDataset(true);
                case 2 -> generateReports();
                case 3 -> back = true;
            }
        }
    }

    /** REQ-0003 to REQ-0005: reads the CSV, keeps the PHP rows, then checks and cleans them. */
    private void loadDataset(boolean verbose)
    {
        System.out.println();
        System.out.println("Processing dataset...");
        try
        {
            TransactionFile.LoadResult result = file.load();
            bank.load(result.transactions());

            System.out.println("(" + result.rowsRead() + " rows loaded, " + result.phpRows() + " filtered for PHP currency)");
            if (verbose || !result.errors().isEmpty() || result.nonPhpRows() > 0)
            {
                System.out.println("Validation: " + result.transactions().size() + " valid rows, "
                        + result.errors().size() + " rows skipped (parse errors or missing values), "
                        + result.nonPhpRows() + " non-PHP rows excluded.");
                showErrors(result.errors());
            }
            System.out.println(bank.getAccounts().size() + " accounts ready.");
        }
        catch (IOException e)
        {
            System.out.println("Could not read " + file.getPath() + ": " + e.getMessage());
        }
    }

    private static void showErrors(List<String> errors)
    {
        for (int i = 0; i < Math.min(errors.size(), MAX_ERRORS_SHOWN); i++)
        {
            System.out.println("  " + errors.get(i));
        }
        if (errors.size() > MAX_ERRORS_SHOWN)
        {
            System.out.println("  ... and " + (errors.size() - MAX_ERRORS_SHOWN) + " more.");
        }
    }

    private void generateReports()
    {
        if (bank.getAccounts().isEmpty())
        {
            System.out.println("No transactions loaded. Choose [1] Load the file first.");
            return;
        }
        System.out.println();
        System.out.println("Generating reports...");
        System.out.println("Outputs saved to individual files...");
        System.out.println();
        try
        {
            reports.generateAll(bank, System.out);
        }
        catch (IOException e)
        {
            System.out.println("Could not write report files: " + e.getMessage());
        }
    }

    // Shared building blocks

    private Account selectAccount()
    {
        if (bank.getAccounts().isEmpty())
        {
            System.out.println("No account registered yet. Choose [1] Register Account first.");
            System.out.println();
            return null;
        }

        String key = Prompt.readText("Enter Account Name or Account Number: ");
        Account account = bank.find(key);
        if (account == null)
        {
            System.out.println("Error: Account '" + key + "' does not exist.");
            System.out.println();
        }
        return account;
    }

    private static void showAccountSummary(Account account)
    {
        System.out.println("Account Name: " + account.getName());
        System.out.println("Account Number: " + account.getNumber());
        System.out.println("Current Balance: " + Formats.plain(account.getBalance()));
        System.out.println("Currency: " + account.getCurrency());
        System.out.println();
    }

    /** Asks for a currency and amount, then gives back the amount converted to the account's currency (PHP). */
    private BigDecimal readTransactionAmount(String action, Account account)
    {
        System.out.println(action + " Currency:");
        showCurrencyMenu();
        ExchangeRates.Currency chosen = Prompt.readCurrency("Select Currency: ");
        BigDecimal amount = Prompt.readPositiveDecimal(action + " Amount (" + chosen + "): ");

        if (!rates.canConvert(chosen, account.getCurrency()))
        {
            System.out.println("Cannot convert " + chosen + " to " + account.getCurrency() + ".");
            System.out.println();
            return null;
        }
        BigDecimal inAccountCurrency = rates.convert(amount, chosen, account.getCurrency());
        if (chosen != account.getCurrency())
        {
            System.out.println("Converted Amount (" + account.getCurrency() + "): " + Formats.plain(inAccountCurrency));
        }
        return inAccountCurrency;
    }
}
