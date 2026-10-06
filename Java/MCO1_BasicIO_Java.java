/********************
Last names: Marcelino, Baun, Cauilan, Dimakuta, 
Language: Java
Paradigm(s): Multi-Paradigm (Object-Oriented, Imperative, Structured)
********************/

import java.util.Scanner; // Import Scanner class for user input
import java.text.DecimalFormat; // Import DecimalFormat class for formatting numbers

public class MCO1_BasicIO_Java {
    public static void main(String[] args) {

        int choice = 0;
        String accountName = "";
        double currentBalance = 1000.00;
        String currentCurrency = "PHP"; // Default currency
        double depositAmount = 0.00;
        double withdrawAmount = 0.00;
        int foreignCurrency = 0; // capture without brackets
        double exchangeRate = 0.00;
        double sourceAmount = 0.00;

        DecimalFormat df = new DecimalFormat("0.00"); // Create DecimalFormat object for formatting numbers

        Scanner sc = new Scanner(System.in); // Initialize Scanner object to read user input
        
        sc.useDelimiter("\\R+"); // Set delimeter to read input until a new line is encountered

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

        System.out.print("Choice: ");
        choice = sc.nextInt();
        System.out.println();
        System.out.println("***");
        System.out.println("Choice = " + choice);
        
        Helpers.printSeparator();

        // Register Account Name

        System.out.println("Register Account Name");
        System.out.print("Account Name: ");
        
        accountName = sc.next().trim(); // read next line and trim
        
        System.out.println();
        System.out.println("***");
        System.out.println("Account Name = " + accountName);

        Helpers.printSeparator();

        //Deposit Amount

        System.out.println("Deposit Amount");
        System.out.print("Account Name: ");
        accountName = sc.next().trim(); // capture account name
        System.out.println("Current Balance: " + df.format(currentBalance));
        System.out.println("Currency: " + currentCurrency);
        System.out.println();
        System.out.print("Deposit Amount: ");
        depositAmount = sc.nextDouble(); // Read deposit amount as double

        System.out.println();
        System.out.println("***");
        System.out.println("Account Name = " + accountName);
        System.out.println("Deposit Amount = " + df.format(depositAmount));
    
        //Withdraw Amount

        Helpers.printSeparator();

        System.out.println("Withdraw Amount");
        System.out.print("Account Name: ");
        accountName = sc.next().trim();
        System.out.println("Current Balance: " + df.format(currentBalance));
        System.out.println("Currency: " + currentCurrency);
        System.out.println();
        System.out.print("Withdraw Amount: ");
        withdrawAmount = sc.nextDouble(); // Read withdraw amount as double

        System.out.println();
        System.out.println("***");
        System.out.println("Account Name = " + accountName);
        System.out.println("Withdraw Amount = " + df.format(withdrawAmount));

        Helpers.printSeparator();

        //Record Exchange Rate
        
        System.out.println("Record Exchange Rate");
        System.out.println();
        System.out.println("[1] Philippine Peso (PHP)");
        System.out.println("[2] United States Dollar (USD)");
        System.out.println("[3] Japanese Yen (JPY)");
        System.out.println("[4] British Pound Sterling (GBP)");
        System.out.println("[5] Euro (EUR)");
        System.out.println("[6] Chinese Yuan Renminni (CNY)");
        System.out.println();
        
        System.out.print("Select Foreign Currency: ");
        foreignCurrency = sc.nextInt(); // Read foreign currency number as int
        System.out.print("Exchange Rate: ");
        exchangeRate = sc.nextDouble(); // Read exchange rate as double

        System.out.println();
        System.out.println("***");
        System.out.println("Select Foreign Currency = [" + foreignCurrency + "]");
        System.out.println("Exchange Rate = " + df.format(exchangeRate));

        Helpers.printSeparator();

        // Currency Exchange

        System.out.println("Foreign Currency Exchange");
        System.out.print("Source Amount (PHP): ");
        sourceAmount = sc.nextDouble(); // Read source amount as double
        System.out.println();
        System.out.println("Exchanged Currency");
        System.out.println("[1] Philippine Peso (PHP) = " + df.format(sourceAmount));
        System.out.println("[2] United States Dollar (USD) = " + df.format(sourceAmount * Helpers.USD_EXCHANGE));
        System.out.println("[3] Japanese Yen (JPY) = " + df.format(sourceAmount * Helpers.JPY_EXCHANGE));
        System.out.println("[4] British Pound Sterling (GBP) = " + df.format(sourceAmount * Helpers.GBP_EXCHANGE));
        System.out.println("[5] Euro (EUR) = " + df.format(sourceAmount * Helpers.EUR_EXCHANGE));
        System.out.println("[6] Chinese Yuan Renminni (CNY) = " + df.format(sourceAmount * Helpers.CNY_EXCHANGE));
        System.out.println();
        System.out.println("***");
        System.out.println("Source Currency = Philippine Peso (PHP)");
        System.out.println("Source Amount (PHP) = " + df.format(sourceAmount));

        sc.close(); 
    }
}