/********************
 * Prompt.java
 * Console input with validation. Every method re-prompts until the answer is valid.
 ********************/

import java.math.BigDecimal;
import java.util.Scanner;

public final class Prompt
{
    private static final Scanner INPUT = new Scanner(System.in);

    private Prompt() { }

    public static String readText(String prompt) // non-empty text
    {
        while (true)
        {
            System.out.print(prompt);
            String line = INPUT.nextLine().trim();
            if (!line.isEmpty()) return line;
            System.out.println("Input cannot be empty.");
        }
    }

    public static int readInt(String prompt, int min, int max) // whole number in range
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
                // falls through to the re-prompt message
            }
            System.out.println("Enter a whole number from " + min + " to " + max + ".");
        }
    }

    public static BigDecimal readPositiveDecimal(String prompt) // amount greater than zero; commas allowed
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
                // falls through to the re-prompt message
            }
            System.out.println("Enter a number greater than zero.");
        }
    }

    public static BigDecimal readDecimalAtLeast(String prompt, BigDecimal minimum)
    {
        while (true)
        {
            BigDecimal value = readPositiveDecimal(prompt);
            if (value.compareTo(minimum) >= 0) return value;
            System.out.println("Amount must be at least " + Formats.plain(minimum) + ".");
        }
    }

    public static ExchangeRates.Currency readCurrency(String prompt)
    {
        int choice = readInt(prompt, 1, ExchangeRates.Currency.values().length);
        return ExchangeRates.Currency.fromMenuNumber(choice);
    }

    public static boolean askYesNo(String prompt)
    {
        while (true)
        {
            String answer = readText(prompt).toUpperCase();
            if (answer.equals("Y")) return true;
            if (answer.equals("N")) return false;
            System.out.println("Please enter Y or N.");
        }
    }
}
