/********************
Last names: Marcelino, Baun, Cauilan, Dimakuta,
Language: Java
Paradigm(s): Multi-Paradigm (Object-Oriented, Imperative, Structured)
********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class MP_11_Java 
{
    private static Scanner INPUT = new Scanner(System.in);
    private static final int MAX_DAYS = 10000; //Arbitrary hard limit for number of days in interest.

    public static void main(String[] args) 
    {
        List<Account> accounts = new ArrayList<>(); //To keep track of accounts during run time.
        ExchangeRates rates = new ExchangeRates();

        boolean running = true;
        while (running) 
        {
            printSeparator();
            showMainMenu();

            switch (readInt("Choice: ", 1, 6)) 
            {
                case 1 -> registerAccountScreen(accounts);
                case 2 -> depositScreen(accounts, rates);
                case 3 -> withdrawScreen(accounts, rates);
                case 4 -> currencyExchangeScreen(rates);
                case 5 -> recordExchangeRateScreen(rates);
                case 6 -> showInterestScreen(accounts);
            }

            running = askYesNo("Back to the Main Menu (Y/N): ");
        }
    }

    public static void printSeparator() 
    {
        System.out.println();
        System.out.println("========================================");
    }

    public static void showMainMenu() 
    {
        System.out.println("Select Transaction:");
        System.out.println("[1] Register Account Name");
        System.out.println("[2] Deposit Amount");
        System.out.println("[3] Withdraw Amount");
        System.out.println("[4] Currency Exchange");
        System.out.println("[5] Record Exchange Rates");
        System.out.println("[6] Show Interest Amount");
        System.out.println();
    }

    public static void showCurrencyMenu() 
    {
        for (ExchangeRates.Currency c:ExchangeRates.Currency.values()) 
        {
            System.out.println("[" + (c.ordinal() + 1) + "]" + c.getLabel());
        }
        System.out.println();
    }

    public static String format(BigDecimal value) 
    {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
    
    //  Input  

    public static String readText(String prompt) // Reads non-empty text input from user
    {
        while (true) 
        {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("Input cannot be empty.");
        }
    }

    public static int readInt(String prompt, int min, int max) // Validates integer within specified range
    {
        while (true) 
        {
            String line = readText(prompt);
            try 
            {
                int value = Integer.parseInt(line);
                if (value >= min && value <= max) return value;
            } 
            catch (NumberFormatException ignored) 
            {
                // Ignores parse error and triggers re-prompt message
            }
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    public static BigDecimal readPositiveDecimal(String prompt) // Validates positive decimal amounts
    {
        while (true) 
        {
            String line = readText(prompt).replace(",", "");
            try 
            {
                BigDecimal value = new BigDecimal(line);
                if (value.signum() > 0) return value;
            } 
            catch (NumberFormatException ignored) 
            {
                // Ignores parse error and triggers re-prompt message
            }
            System.out.println("Enter a number greater than zero.");
        }
    }

    public static ExchangeRates.Currency readCurrency(String prompt) // Maps user choice to valid Currency enum
    {
        int choice = readInt(prompt, 1, ExchangeRates.Currency.values().length);
        return ExchangeRates.Currency.fromMenuNumber(choice);
    }

    public static boolean askYesNo(String prompt) // Reads Y/N user response
    {
        while (true) 
        {
            String answer = readText(prompt).toUpperCase();
            if (answer.equals("Y")) return true;
            if (answer.equals("N")) return false;
            System.out.println("Please enter Y or N.");
        }
    }

    //  Transaction screens 

    public static void registerAccountScreen(List<Account> accounts) 
    {
        System.out.println("Register Account Name");
        String name = readText("Account Name: ");
        Account newAccount = new Account();
        newAccount.register(name);
        accounts.add(newAccount);
        System.out.println();
    }

    public static void depositScreen(List<Account> accounts, ExchangeRates rates) 
    {
        System.out.println("Deposit Amount");
        Account account = selectAccount(accounts);
        if (account == null) return;
        
        showAccountSummary(account);
        BigDecimal amount = readTransactionAmount("Deposit", account, rates);
        if (amount == null) return;
        
        try 
        {
            account.deposit(amount);
            System.out.println("Updated Balance: " + format(account.getBalance()));
        } 
        catch (IllegalArgumentException e) // Catches invalid deposit amounts and displays message
        {
            System.out.println("Deposit failed: " + e.getMessage());
        }
        System.out.println();
    }

    public static void withdrawScreen(List<Account> accounts, ExchangeRates rates) 
    {
        System.out.println("Withdraw Amount");
        Account account = selectAccount(accounts);
        if (account == null) return;

        showAccountSummary(account);
        BigDecimal amount = readTransactionAmount("Withdraw", account, rates);
        if (amount == null) return;
        
        try 
        {
            account.withdraw(amount);
            System.out.println("Updated Balance: " + format(account.getBalance()));
        } 
        catch (IllegalArgumentException e) 
        {
            System.out.println("Withdrawal failed: " + e.getMessage()); // Catches insufficient balance error
        }
        System.out.println();
    }

    public static void currencyExchangeScreen(ExchangeRates rates)
    {
        do
        {
            System.out.println("Foreign Currency Exchange");
            System.out.println("Source Currency Option:");
            showCurrencyMenu();
            ExchangeRates.Currency source = readCurrency("Source Currency: ");
            BigDecimal amount = readPositiveDecimal("Source Amount: ");
            System.out.println();

            System.out.println("Exchanged Currency Options:");
            showCurrencyMenu();
            ExchangeRates.Currency target = readCurrency("Exchange Currency: ");

            if (rates.canConvert(source, target)) 
            {
                System.out.println("Exchange Amount: " + format(rates.convert(amount, source, target)));
            } 
            else
            {
                System.out.println("Cannot convert currencies.");
            }

            System.out.println();

        } while (askYesNo("Convert another currency (Y/N)? "));
    }

    public static void recordExchangeRateScreen(ExchangeRates rates) 
    {
        System.out.println("Record Exchange Rate");
        showCurrencyMenu();

        ExchangeRates.Currency currency = readCurrency("Select Foreign Currency: ");
        if (currency == ExchangeRates.BASE) 
        {
            System.out.println(currency + " is the base currency; its rate is fixed at 1.00.");
            System.out.println();
            return;
        }
        BigDecimal rate = readPositiveDecimal("Exchange Rate: ");
        rates.setRate(currency, rate);
        System.out.println();
    }

    public static void showInterestScreen(List<Account> accounts) 
    {
        System.out.println("Show Interest Amount");
        Account account = selectAccount(accounts);
        if (account == null) return;

        showAccountSummary(account);
        System.out.println("Interest Rate: " + Account.ANNUAL_INTEREST_RATE
                .multiply(new BigDecimal("100")).stripTrailingZeros().toPlainString() + "%");
        System.out.println();

        int days = readInt("Total Number of Days: ", 1, MAX_DAYS);
        System.out.println();

        List<Account.InterestEntry> table = account.projectInterest(days);
        System.out.println("Day  | Interest  | Balance    |");
        for (Account.InterestEntry row : table) {
        System.out.printf("%-4d | %-9s | %-10s |%n", row.getDay(), format(row.getInterest()), format(row.getBalance()));
        }
        System.out.println();
    }

    //  Private building blocks 

    private static Account selectAccount(List<Account> accounts) 
    {
        if (accounts.isEmpty()) 
        {
            System.out.println("No account registered yet. Choose [1] Register Account Name first.");
            System.out.println();
            return null;
        }
        
        String inputName = readText("Enter Account Name: ");
        for (Account account : accounts) 
        {
            // Changed from equalsIgnoreCase to equals for case-sensitivity
            if (account.getName().equals(inputName)) 
            {
                return account;
            }
        }
        
        System.out.println("Error: Account '" + inputName + "' does not exist.");
        System.out.println();
        return null;
    }

    private static void showAccountSummary(Account account) 
    {
        System.out.println("Account Name: " + account.getName());
        System.out.println("Current Balance: " + format(account.getBalance()));
        System.out.println("Currency: " + account.getCurrency());
        System.out.println();
    }

    private static BigDecimal readTransactionAmount(String action, Account account, ExchangeRates rates) 
    {
        System.out.println(action + " Currency:");
        showCurrencyMenu();
        ExchangeRates.Currency chosen = readCurrency("Select Currency: ");
        BigDecimal amount = readPositiveDecimal(action + " Amount (" + chosen + "): ");

        if (!rates.canConvert(chosen, account.getCurrency())) 
        {
            System.out.println("Cannot convert " + chosen + " to " + account.getCurrency() + ".");
            System.out.println();
            return null;
        }
        BigDecimal inAccountCurrency = rates.convert(amount, chosen, account.getCurrency());
        if (chosen != account.getCurrency()) 
        {
            System.out.println("Converted Amount (" + account.getCurrency() + "): " + format(inAccountCurrency));
        }
        return inAccountCurrency;
    }
}