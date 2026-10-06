import java.util.Scanner; // Import Scanner class for user input
import java.math.BigDecimal; // Import BigDecimal class for precise decimal calculations

public class MCO1_BasicIO_Java {
    public static void main(String[] args) {

        int choice = 0;
        String accountName = "";
        BigDecimal currentBalance = new BigDecimal("1000.00"); 
        String currentCurrency = "PHP"; // Default currency
        BigDecimal depositAmount = new BigDecimal("0.00");

        Scanner sc = new Scanner(System.in); // Initialize Scanner object to read user input
        
        Helpers.printSeparator();

        //Display option block for user to select transaction
        System.out.println("Select Transaction:");
        System.out.println("[1] Register Account Name");
        System.out.println("[2] Deposit Amount");
        System.out.println("[3] Withdraw Amount");
        System.out.println("[4] Currency Exchange");
        System.out.println("[5] Record Exchange Rates");
        System.out.println("[6] Show Interest Amount");
        System.out.println();

        // Prompt user for choice
        System.out.print("Choice: ");
        choice = sc.nextInt();
        System.out.println();
        System.out.println("***");
        System.out.println("Choice = " + choice);
        
        Helpers.printSeparator();

        // Register Account Name

        System.out.println("Register Account Name");
        System.out.print("Account Name: ");
        
        accountName = sc.useDelimiter( "\n").next(); // Read account name with delimeter
        accountName = accountName.trim(); // Trim whitespace from account name
        
        System.out.println();
        System.out.println("***");
        System.out.println("Account Name = " + accountName);

        Helpers.printSeparator();

        System.out.println("Deposit Amount");
        System.out.println("Account Name: " + accountName);
        System.out.println("Current Balance: " + currentBalance);
        System.out.println("Currency: " + currentCurrency);
        System.out.println();
        System.out.print("Deposit Amount: ");
        depositAmount = sc.nextBigDecimal(); // Read deposit amount as BigDecimal

        System.out.println();
        System.out.println("***");
        System.out.println("Account Name = " + accountName);
        System.out.println("Deposit Amount = " + depositAmount);

        sc.close(); 
    }
}