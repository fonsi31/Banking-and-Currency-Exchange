import java.util.Scanner; // Import Scanner class for user input

public class Main {
    public static void main(String[] args) {

        int choice = 0;
        String accountName = "";

        Scanner sc = new Scanner(System.in); // Initialize Scanner object to read user input

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
        
        sc.close(); 
    }
}