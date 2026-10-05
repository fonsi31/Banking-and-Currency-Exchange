public class Main {
    public static void main(String[] args) {
        
        Scanner sc = new Scanner(System.in); // Initialize Scanner object to read user input

        //Display option block for user to select transaction
        System.out.println("Select Transaction:");
        System.out.println("[1] Register Account Name");
        System.out.println("[2] Deposit Amount");
        System.out.println("[3] Withdraw Amount");
        System.out.println("[4] Currency Exchange");
        System.out.println("[5] Record Exchange Rates");
        System.out.println("[6] Show Interest Amount");

        // Prompt user for choice
        System.out.print("Enter your choice: ");
        int choice = sc.nextInt();
    }
}