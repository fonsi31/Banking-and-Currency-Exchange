/********************
 * Account.java
 * Account handling: owner name, balance, currency, deposits, withdrawals
 * and interest computation.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

public class Account {

    public static final BigDecimal OPENING_BALANCE = new BigDecimal("1000.00"); //1000 peso base
    public static final BigDecimal ANNUAL_INTEREST_RATE = new BigDecimal("0.05"); // 5% per annum
    private static final BigDecimal DAYS_IN_YEAR = new BigDecimal("365");

    private String name;                      
    private BigDecimal balance;
    private final ExchangeRates.Currency currency;

    public Account() 
    {
        this.name = null;
        this.balance = OPENING_BALANCE; 
        this.currency = ExchangeRates.BASE;   
    }

    public static class InterestEntry {
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

    public void register(String accountName)
    {
        if (accountName == null || accountName.trim().isEmpty())
        {
            throw new IllegalArgumentException("Account name cannot be empty.");
        }
        this.name = accountName.trim();
    }

    public boolean isRegistered()
    {
        return name != null;
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
        return currency;
    }

    public void deposit(BigDecimal amount) 
    {
        balance = balance.add(toValidMoney(amount));
    }

    public void withdraw(BigDecimal amount) 
    {
        BigDecimal money = toValidMoney(amount);
        if (money.compareTo(balance) > 0) 
        {
            throw new IllegalArgumentException("Insufficient balance.");
        }
        balance = balance.subtract(money);
    }

    public static BigDecimal computeDailyInterest(BigDecimal endOfDayBalance) 
    {
        return endOfDayBalance.multiply(ANNUAL_INTEREST_RATE).divide(DAYS_IN_YEAR, 2, RoundingMode.HALF_UP);
    }

    public List<InterestEntry> projectInterest(int days) {
        if (days < 1)
        {
            throw new IllegalArgumentException("Number of days must be at least 1.");
        }

        List<InterestEntry> table = new ArrayList<>();
        BigDecimal running = balance;
        for (int day = 1; day <= days; day++) 
        {
            BigDecimal interest = computeDailyInterest(running);
            running = running.add(interest);
            table.add(new InterestEntry(day, interest, running));
        }
        return table;
    }

    private static BigDecimal toValidMoney(BigDecimal amount) 
    {
        if (amount == null) 
        {
            throw new IllegalArgumentException("Amount is required.");
        }
        BigDecimal money = amount.setScale(2, RoundingMode.HALF_UP); // Standardizes amount to 2 decimal places (centavos)
        if (money.signum() <= 0) 
        {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }
        return money;
    }
}