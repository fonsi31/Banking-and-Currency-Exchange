/********************
 * Transaction.java
 * One row of bank_transactions.csv (PHP only). It never changes once made.
 ********************/

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record Transaction(String accountName,
                          String accountNumber,
                          LocalDateTime timestamp,
                          Type type,
                          BigDecimal amount,
                          BigDecimal balanceAfter)
{
    public enum Type
    {
        DEPOSIT("deposit"),
        WITHDRAWAL("withdrawal");

        private final String label;

        Type(String label)
        {
            this.label = label;
        }

        public String label()
        {
            return label;
        }

        public static Type fromLabel(String text)
        {
            for (Type t : values())
            {
                if (t.label.equalsIgnoreCase(text.trim())) return t;
            }
            throw new IllegalArgumentException("unknown transaction type '" + text + "'");
        }
    }

    public LocalDate date()
    {
        return timestamp.toLocalDate();
    }

    public boolean isDeposit()
    {
        return type == Type.DEPOSIT;
    }
}
