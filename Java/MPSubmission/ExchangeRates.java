/********************
 * ExchangeRates.java
 * Stores the recorded exchange rates, fixes PHP as the base
 * currency, and converts between currencies.
 ********************/

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap; //Using enum map of the currency to get easier access to each one.
import java.util.Map;

public class ExchangeRates 
{

    public enum Currency 
    {
        PHP("Philippine Peso"),
        USD("United States Dollar"),
        JPY("Japanese Yen"),
        GBP("British Pound Sterling"),
        EUR("Euro"),
        CNY("Chinese Yuan Renminni");

        private final String fullName;

        Currency(String fullName) 
        { 
            this.fullName = fullName; 
        }

        public String getLabel() 
        { 
            return fullName + " (" + name() + ")"; 
        }

        public static Currency fromMenuNumber(int number) 
        { 
            return values()[number - 1]; 
        }
    }

    public static final Currency BASE = Currency.PHP;
    private static final int WORKING_SCALE = 10;
    private final Map<Currency, BigDecimal> ratesToBase = new EnumMap<>(Currency.class);

    public ExchangeRates() 
    {
        ratesToBase.put(BASE, BigDecimal.ONE); 
        // Default rates
        ratesToBase.put(Currency.USD, new BigDecimal("62.00"));
        ratesToBase.put(Currency.JPY, new BigDecimal("0.40"));
        ratesToBase.put(Currency.GBP, new BigDecimal("84.00"));
        ratesToBase.put(Currency.EUR, new BigDecimal("72.00"));
        ratesToBase.put(Currency.CNY, new BigDecimal("9.00"));
    }

    public void setRate(Currency currency, BigDecimal rate) 
    {
        if (currency == BASE) 
        {
            throw new IllegalArgumentException(BASE + " is the base currency; its rate is fixed at 1.00.");
        }
        if (rate == null || rate.signum() <= 0) 
        {
            throw new IllegalArgumentException("Exchange rate must be greater than zero.");
        }
        ratesToBase.put(currency, rate);
    }

    public boolean hasRate(Currency currency) 
    { 
        return ratesToBase.containsKey(currency); 
    }
    
    public BigDecimal getRate(Currency currency) 
    { 
        return ratesToBase.get(currency); 
    }

    public boolean canConvert(Currency from, Currency to) 
    {
        return from == to || (hasRate(from) && hasRate(to)); // Checks if both currencies have recorded rates available
    }

    public BigDecimal convert(BigDecimal amount, Currency from, Currency to) 
    {
        if (!canConvert(from, to)) 
        {
            throw new IllegalStateException("Exchange rate not recorded.");
        }
        if (from == to) 
        {
            return amount.setScale(2, RoundingMode.HALF_UP);
        }
        
        BigDecimal inBase = amount.multiply(ratesToBase.get(from)); // Converts source currency amount to base currency (PHP)
        return inBase.divide(ratesToBase.get(to), WORKING_SCALE, RoundingMode.HALF_UP).setScale(2, RoundingMode.HALF_UP);
        // Converts base currency value to target currency rounded to 2 decimal places
    }
}