/********************
 * Bank.java
 * Holds every registered account and performs registration, deposits and
 * withdrawals. Each change is saved to bank_transactions.csv before it is
 * applied in memory, so the file and the program never disagree.
 ********************/

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class Bank
{
    public static final BigDecimal MINIMUM_INITIAL_DEPOSIT = new BigDecimal("5000.00");
    private static final long FIRST_ACCOUNT_NUMBER = 1092000001L;
    // "First name Last name": two or more words made of letters, apostrophes, periods or hyphens.
    private static final Pattern FULL_NAME = Pattern.compile("\\p{L}[\\p{L}'.-]*(\\s+\\p{L}[\\p{L}'.-]*)+");

    private final TransactionFile file;
    private final Map<String, Account> accounts = new LinkedHashMap<>(); // keyed by account number
    private long highestNumber = FIRST_ACCOUNT_NUMBER - 1;

    public Bank(TransactionFile file)
    {
        this.file = file;
    }

    // Loading

    /** Replaces everything in memory with the given (already cleaned and sorted) transactions. */
    public void load(Collection<Transaction> transactions)
    {
        accounts.clear();
        highestNumber = FIRST_ACCOUNT_NUMBER - 1;
        for (Transaction t : transactions)
        {
            Account account = accounts.computeIfAbsent(t.accountNumber(), n -> new Account(n, t.accountName()));
            account.record(t);
            highestNumber = Math.max(highestNumber, Long.parseLong(t.accountNumber()));
        }
    }

    // Queries

    public Collection<Account> getAccounts()
    {
        return Collections.unmodifiableCollection(accounts.values());
    }

    public Stream<Transaction> transactions()
    {
        return accounts.values().stream().flatMap(a -> a.getHistory().stream());
    }

    /** Finds an account by its 10-digit number or by its exact (case-sensitive) name. */
    public Account find(String nameOrNumber)
    {
        Account byNumber = accounts.get(nameOrNumber);
        if (byNumber != null) return byNumber;
        return accounts.values().stream().filter(a -> a.getName().equals(nameOrNumber)).findFirst().orElse(null);
    }

    /** Date of the most recent transaction in the data; the "today" that reports are measured from. */
    public LocalDate latestActivityDate()
    {
        return accounts.values().stream()
                .map(Account::lastActivity)
                .max(LocalDateTime::compareTo)
                .map(LocalDateTime::toLocalDate)
                .orElse(null);
    }

    public long daysSinceOpened(Account account, LocalDate asOf)
    {
        return ChronoUnit.DAYS.between(account.openedOn(), asOf);
    }

    // Commands

    /** Validates an account name and returns it tidied (trimmed, single spaces). Throws if unusable. */
    public String checkName(String rawName)
    {
        String name = rawName.trim().replaceAll("\\s+", " ");
        if (!FULL_NAME.matcher(name).matches())
        {
            throw new IllegalArgumentException("Account name must be in the form \"First name Last name\".");
        }
        if (accounts.values().stream().anyMatch(a -> a.getName().equalsIgnoreCase(name)))
        {
            throw new IllegalArgumentException("Account '" + name + "' is already registered.");
        }
        return name;
    }

    /** REQ-0001: registers a new account with the next account number and an initial deposit. */
    public Account register(String rawName, BigDecimal initialDeposit) throws IOException
    {
        String name = checkName(rawName);
        if (initialDeposit.compareTo(MINIMUM_INITIAL_DEPOSIT) < 0)
        {
            throw new IllegalArgumentException("Initial deposit must be at least " + Formats.plain(MINIMUM_INITIAL_DEPOSIT) + ".");
        }

        String number = String.format("%010d", highestNumber + 1);
        Account account = new Account(number, name);
        account.record(save(account, Transaction.Type.DEPOSIT, initialDeposit));
        accounts.put(number, account);
        highestNumber++;
        return account;
    }

    public void deposit(Account account, BigDecimal amount) throws IOException
    {
        account.record(save(account, Transaction.Type.DEPOSIT, amount));
    }

    public void withdraw(Account account, BigDecimal amount) throws IOException
    {
        account.record(save(account, Transaction.Type.WITHDRAWAL, amount));
    }

    /** REQ-0002: builds the transaction, appends it to the CSV, and returns it for the caller to apply. */
    private Transaction save(Account account, Transaction.Type type, BigDecimal amount) throws IOException
    {
        // Never earlier than the account's last transaction, so its history stays in order.
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime when = now.isAfter(account.lastActivity()) ? now : account.lastActivity();

        Transaction t = account.newTransaction(type, amount, when);
        file.append(t);
        return t;
    }
}
