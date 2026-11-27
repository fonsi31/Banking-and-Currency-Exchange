/********************
 * Account.java
 * One bank account: its identity, balance and full transaction history.
 * The balance is always the CurrentBalance of the latest transaction.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class Account
{
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.05"); // 5% per annum
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private final String number;
    private final String name;
    private BigDecimal balance = BigDecimal.ZERO;
    private final List<Transaction> history = new ArrayList<>();                      // chronological
    private final NavigableMap<LocalDate, BigDecimal> endOfDayBalances = new TreeMap<>(); // last balance of each active day

    public Account(String number, String name)
    {
        this.number = number;
        this.name = name;
    }

    public static class InterestEntry
    {
        private final int day;
        private final BigDecimal interest;
        private final BigDecimal balance;

        InterestEntry(int day, BigDecimal interest, BigDecimal balance)
        {
            this.day = day;
            this.interest = interest;
            this.balance = balance;
        }

        public int getDay()
        {
            return day;
        }

        public BigDecimal getInterest()
        {
            return interest;
        }

        public BigDecimal getBalance()
        {
            return balance;
        }
    }

    // Accessors

    public String getNumber()
    {
        return number;
    }

    public String getName()
    {
        return name;
    }

    public BigDecimal getBalance()
    {
        return balance;
    }

    public ExchangeRates.Currency getCurrency()
    {
        return ExchangeRates.BASE;
    }

    public List<Transaction> getHistory()
    {
        return Collections.unmodifiableList(history);
    }

    public LocalDate openedOn()
    {
        return history.isEmpty() ? null : history.get(0).date();
    }

    public LocalDateTime lastActivity()
    {
        return history.isEmpty() ? LocalDateTime.MIN : history.get(history.size() - 1).timestamp();
    }

    // Transactions

    /**
     * Builds the transaction a deposit or withdrawal would create, without applying it.
     * The caller saves it and then calls record(), so a failed save leaves the account untouched.
     */
    public Transaction newTransaction(Transaction.Type type, BigDecimal amount, LocalDateTime when)
    {
        BigDecimal money = toValidMoney(amount);
        BigDecimal after;

        if (type == Transaction.Type.DEPOSIT)
        {
            after = balance.add(money);
        }
        else
        {
            if (money.compareTo(balance) > 0)
            {
                throw new IllegalArgumentException("Insufficient balance.");
            }
            after = balance.subtract(money);
        }
        return new Transaction(name, number, when, type, money, after);
    }

    /** Applies a transaction (new or loaded from file). Transactions must arrive in chronological order. */
    public void record(Transaction t)
    {
        history.add(t);
        balance = t.balanceAfter();
        endOfDayBalances.put(t.date(), t.balanceAfter()); // later transactions on the same day overwrite
    }

    /** End-of-Day balance on a date; carries forward over idle days, 0 before the account was opened. */
    public BigDecimal endOfDayBalance(LocalDate date)
    {
        Map.Entry<LocalDate, BigDecimal> entry = endOfDayBalances.floorEntry(date);
        return entry == null ? BigDecimal.ZERO : entry.getValue();
    }

    // Interest

    public static BigDecimal computeDailyInterest(BigDecimal endOfDayBalance)
    {
        return endOfDayBalance.multiply(ANNUAL_INTEREST_RATE).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);
    }

    public List<InterestEntry> projectInterest(int days)
    {
        if (days < 1)
        {
            throw new IllegalArgumentException("Number of days must be at least 1.");
        }

        List<InterestEntry> table = new ArrayList<>(days);
        BigDecimal running = balance;
        for (int day = 1; day <= days; day++)
        {
            BigDecimal interest = computeDailyInterest(running);
            running = running.add(interest);
            table.add(new InterestEntry(day, interest, running));
        }
        return table;
    }

    /** Total compounded interest earned over the given number of days (no table built). */
    public BigDecimal expectedInterest(int days)
    {
        BigDecimal running = balance;
        for (int day = 1; day <= days; day++)
        {
            running = running.add(computeDailyInterest(running));
        }
        return running.subtract(balance);
    }

    private static BigDecimal toValidMoney(BigDecimal amount)
    {
        if (amount == null)
        {
            throw new IllegalArgumentException("Amount is required.");
        }
        BigDecimal money = amount.setScale(2, RoundingMode.HALF_UP); // centavo precision
        if (money.signum() <= 0)
        {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return money;
    }
}
