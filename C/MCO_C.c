/*******************
Last names: Dimakuta, Cauilan, Marcelino, Baun
Language: C
Paradigm(s): Procedural
*******************/

#include <stdio.h>
#include <string.h>

// Structure to track users active banking session
typedef struct {
    char accountName[100];
    double balance;
    int isRegistered; // 0 for unregistered, 1 for registered
} BankAccount;

// Array storing the exchange rates, PHP is always 1.00 at 0 index
// Indices are 0 = PHP, 1 = USD, 2 = JPY, 3 = GBP, 4 = EUR, 5 = CNY
double exchangeRates[6] = {1.00, 62.00, 0.40, 84.00, 72.00, 9.00};
char* currencyNames[6] = {
    "Philippine Peso (PHP)",
    "United States Dollar (USD)",
    "Japanese Yen (JPY)",
    "British Pound Sterling (GBP)",
    "Euro (EUR)",
    "Chinese Yuan Renminni (CNY)"
};

// Function prototypes
void displayMenu();
void clearInputBuffer();
int askToReturn();
void registerAccount(BankAccount *acc);
void deposit(BankAccount *acc);
void withdraw(BankAccount *acc);
void currencyExchange();
void recordExchangeRate();
void showInterestAmount(BankAccount *acc);

int main(void) {
    int choice;
    BankAccount userAccount;
    userAccount.balance = 0.0;
    userAccount.isRegistered = 0;
    
    // Main program loop
    while(1) {
        displayMenu();
        if (scanf("%d", &choice) != 1) {
            clearInputBuffer();
            printf("Invalid input. Please try again.\n");
            continue;
        }
        
        switch(choice) {
            case 1:
                registerAccount(&userAccount);
                break;
            case 2:
                deposit(&userAccount);
                break;
            case 3:
                withdraw(&userAccount);
                break;
            case 4:
                currencyExchange();
                break;
            case 5:
                recordExchangeRate();
                break;
            case 6:
                showInterestAmount(&userAccount);
                break;
            case 7: // Added exit option to break loop
                printf("\nThank you for using the Banking Application.\n");
                return 0;
            default:
                printf("\nInvalid choice. Please select 1-7.\n\n");
                break;
        }
    }
    
    return 0;
}

// Clears hanging newline characters so scanf works properly
void clearInputBuffer() {
    int c;
    while ((c = getchar()) != '\n' && c != EOF) { }
}

// Reusable function to pause before returning to main menu
int askToReturn() {
    char ans;
    printf("\nBack to the Main Menu (Y/N): ");
    scanf(" %c", &ans);
    return (ans == 'Y' || ans == 'y');
}

void displayMenu() {
    printf("======================================\n");
    printf("Select Transaction:\n");
    printf("[1] Register Account Name\n");
    printf("[2] Deposit Amount\n");
    printf("[3] Withdraw Amount\n");
    printf("[4] Currency Exchange\n");
    printf("[5] Record Exchange Rates\n");
    printf("[6] Show Interest Amount\n");
    printf("[7] Exit\n\n");
    printf("Choice: ");
}

void registerAccount(BankAccount *acc) {
    printf("\nRegister Account Name\n");
    printf("Account Name: ");
    
    clearInputBuffer();
    scanf(" %99[^\n]", acc->accountName);
    
    // Initialize starting balance and flag as registered
    acc->balance = 1000.00; 
    acc->isRegistered = 1;
    
    askToReturn();
}

void deposit(BankAccount *acc) {
    double depositAmount;
    printf("\nDeposit Amount\n");
    
    if (!acc->isRegistered) {
        printf("Please register an account first.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc->accountName);
    printf("Current Balance: %.2f\n", acc->balance);
    printf("Currency: PHP\n\n");
    printf("Deposit Amount: ");
    scanf("%lf", &depositAmount);
    
    if (depositAmount > 0) {
        acc->balance += depositAmount; // Update struct state
        printf("Updated Balance: %.2f\n", acc->balance);
    } else {
        printf("Invalid deposit amount.\n");
    }
    
    askToReturn();
}

void withdraw(BankAccount *acc) {
    double withdrawAmount;
    printf("\nWithdraw Amount\n");
    
    if (!acc->isRegistered) {
        printf("Please register an account first.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc->accountName);
    printf("Current Balance: %.2f\n", acc->balance);
    printf("Currency: PHP\n\n");
    printf("Withdraw Amount: ");
    scanf("%lf", &withdrawAmount);
    
    if (withdrawAmount > 0 && withdrawAmount <= acc->balance) {
        acc->balance -= withdrawAmount; // Subtract from struct state
        printf("Updated Balance: %.2f\n", acc->balance);
    } else if (withdrawAmount > acc->balance) {
        printf("Insufficient balance.\n");
    } else {
        printf("Invalid withdraw amount.\n");
    }
    
    askToReturn();
}

void currencyExchange() {
    double sourceAmount;
    char convertAnother;
    
    do {
        printf("\nForeign Currency Exchange\n");
        printf("Source Currency Option:\n");
        for(int i = 0; i < 6; i++) {
            printf("[%d] %s\n", i + 1, currencyNames[i]);
        }
        
        int sourceChoice;
        printf("\nSource Currency: ");
        scanf("%d", &sourceChoice);
        
        printf("Source Amount: ");
        scanf("%lf", &sourceAmount);
        
        printf("\nExchanged Currency Options:\n");
        for(int i = 0; i < 6; i++) {
            printf("[%d] %s\n", i + 1, currencyNames[i]);
        }
        
        int exchChoice;
        printf("\nExchange Currency: ");
        scanf("%d", &exchChoice);
        
        // Math converts input to PHP base, then to target currency
        double amountInPHP = sourceAmount;
        if(sourceChoice != 1) {
            amountInPHP = sourceAmount * exchangeRates[sourceChoice - 1];
        }
        
        double finalAmount = amountInPHP;
        if(exchChoice != 1) {
            finalAmount = amountInPHP / exchangeRates[exchChoice - 1];
        }
        
        printf("Exchange Amount: %.2f\n\n", finalAmount);
        printf("Convert another currency (Y/N)? ");
        scanf(" %c", &convertAnother);
        
    } while(convertAnother == 'Y' || convertAnother == 'y');
}

void recordExchangeRate() {
    int currChoice;
    double newRate;
    
    printf("\nRecord Exchange Rate\n");
    for(int i = 0; i < 6; i++) {
        printf("[%d] %s\n", i + 1, currencyNames[i]);
    }
    
    printf("\nSelect Foreign Currency: ");
    scanf("%d", &currChoice);
    
    if(currChoice == 1) {
        printf("PHP is the base currency and must remain 1.00.\n");
    } else if(currChoice >= 2 && currChoice <= 6) {
        printf("Exchange Rate: ");
        scanf("%lf", &newRate);
        if(newRate > 0) {
            // Update array value
            exchangeRates[currChoice - 1] = newRate; 
            printf("Rate updated successfully.\n");
        } else {
            printf("Invalid rate.\n");
        }
    } else {
        printf("Invalid currency selection.\n");
    }
    
    askToReturn();
}

void showInterestAmount(BankAccount *acc) {
    int days;
    printf("\nShow Interest Amount\n");
    
    if (!acc->isRegistered) {
        printf("Please register an account first.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc->accountName);
    printf("Current Balance: %.2f\n", acc->balance);
    printf("Currency: PHP\n");
    printf("Interest Rate: 5%%\n\n");
    
    printf("Total Number of Days: ");
    scanf("%d", &days);
    
    printf("\nDay | Interest | Balance |\n");
    
    double simulatedBalance = acc->balance;
    for(int i = 1; i <= days; i++) {
        // Daily Interest = (End-of-Day Balance) x (Annual Interest Rate / 365)
        double dailyInterest = simulatedBalance * (0.05 / 365.0);
        simulatedBalance += dailyInterest;
        
        printf("%-3d | %-8.2f | %.2f |\n", i, dailyInterest, simulatedBalance);
    }
    
    askToReturn();
}
