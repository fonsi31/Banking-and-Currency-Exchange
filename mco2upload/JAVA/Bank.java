/********************
 * Bank.java
 * Keeps all the registered accounts and handles registration, deposits and
 * withdrawals. Every change gets saved to bank_transactions.csv first, then
 * applied in memory, so the file and the program always match.
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
    // "First name Last name": two or more words, only letters, apostrophes, periods or hyphens.
    private static final Pattern FULL_NAME = Pattern.compile("\\p{L}[\\p{L}'.-]*(\\s+\\p{L}[\\p{L}'.-]*)+");

    private final TransactionFile file;
    private final Map<String, Account> accounts = new LinkedHashMap<>(); // key is the account number
    private long highestNumber = FIRST_ACCOUNT_NUMBER - 1;

    public Bank(TransactionFile file)
    {
        this.file = file;
    }

    // Loading

    /** Wipes what's in memory and reloads it from the given transactions (already cleaned and sorted). */
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

    /** Looks up an account by its 10-digit number or its exact name (case-sensitive). */
    public Account find(String nameOrNumber)
    {
        Account byNumber = accounts.get(nameOrNumber);
        if (byNumber != null) return byNumber;
        return accounts.values().stream().filter(a -> a.getName().equals(nameOrNumber)).findFirst().orElse(null);
    }

    /** Date of the latest transaction in the data. This is the "today" the reports count from. */
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

    /** Checks the account name and returns it cleaned up (trimmed, single spaces). Throws if it's no good. */
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

    /** REQ-0001: registers a new account with the next account number plus the initial deposit. */
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

    /** REQ-0002: builds the transaction, appends it to the CSV, then hands it back so the caller can apply it. */
    private Transaction save(Account account, Transaction.Type type, BigDecimal amount) throws IOException
    {
        // Never earlier than the account's last transaction, so the history stays in order.
        LocalDateTime now = LocalDateTime.now().withNano(0);
        LocalDateTime when = now.isAfter(account.lastActivity()) ? now : account.lastActivity();

        Transaction t = account.newTransaction(type, amount, when);
        file.append(t);
        return t;
    }
}
