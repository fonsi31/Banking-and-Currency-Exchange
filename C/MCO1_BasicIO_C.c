/*******************
Last names: Dimakuta, Cauilan, Marcelino, Baun
Language: C
Paradigm(s): Procedural
*******************/

#include <stdio.h>

int main(void) {
    int choice;
    char accountName[100];
    double depositAmount;
    double withdrawAmount;
    char foreignCurrency[50];
    double exchangeRate;
    double sourceAmount;

    // Main Menu
    printf("Select Transaction:\n");
    printf("[1] Register Account Name\n");
    printf("[2] Deposit Amount\n");
    printf("[3] Withdraw Amount\n");
    printf("[4] Currency Exchange\n");
    printf("[5] Record Exchange Rates\n");
    printf("[6] Show Interest Amount\n\n");
    printf("Choice: ");
    scanf("%d", &choice);
    printf("\n***\nChoice = %d\n\n", choice);

    // Register
    printf("Register Account Name\n");
    printf("Account Name: ");
    scanf(" %99[^\n]", accountName);
    printf("\n***\nAccount Name = %s\n\n", accountName);

    // Deposit
    printf("Deposit Amount\n");
    printf("Account Name: ");
    scanf(" %99[^\n]", accountName);
    printf("Current Balance: 1000.00\n");
    printf("Currency: PHP\n\n");
    printf("Deposit Amount: ");
    scanf("%lf", &depositAmount);
    printf("\n***\nAccount Name = %s\n", accountName);
    printf("Deposit Amount = %.2f\n\n", depositAmount);

    // Withdraw
    printf("Withdraw Amount\n");
    printf("Account Name: ");
    scanf(" %99[^\n]", accountName);
    printf("Current Balance: 1000.00\n");
    printf("Currency: PHP\n\n");
    printf("Withdraw Amount: ");
    scanf("%lf", &withdrawAmount);
    printf("\n***\nAccount Name = %s\n", accountName);
    printf("Withdraw Amount = %.2f\n\n", withdrawAmount);

    // Exchange Rate
    printf("Record Exchange Rate\n\n");
    printf("[1] Philippine Peso (PHP)\n");
    printf("[2] United States Dollar (USD)\n");
    printf("[3] Japanese Yen (JPY)\n");
    printf("[4] British Pound Sterling (GBP)\n");
    printf("[5] Euro (EUR)\n");
    printf("[6] Chinese Yuan Renminni (CNY)\n\n");
    printf("Select Foreign Currency: ");
    scanf(" %49[^\n]", foreignCurrency);
    printf("Exchange Rate: ");
    scanf("%lf", &exchangeRate);
    printf("\n***\nSelect Foreign Currency = %s\n", foreignCurrency);
    printf("Exchange Rate = %.2f\n\n", exchangeRate);

    // Foreign Exchange
    printf("Foreign Currency Exchange\n");
    printf("Source Amount (PHP): ");
    scanf("%lf", &sourceAmount);
    printf("\nExchanged Currency\n");
    printf("[1] Philippine Peso (PHP) = %.2f\n", sourceAmount);
    printf("[2] United States Dollar (USD) = %.2f\n", sourceAmount * 62.00);
    printf("[3] Japanese Yen (JPY) = %.2f\n", sourceAmount * 0.40);
    printf("[4] British Pound Sterling (GBP) = %.2f\n", sourceAmount * 84.00);
    printf("[5] Euro (EUR) = %.2f\n", sourceAmount * 72.00);
    printf("[6] Chinese Yuan Renminni (CNY) = %.2f\n\n", sourceAmount * 9.00);
    printf("***\nSource Currency = Philippine Peso (PHP)\n");
    printf("Source Amount (PHP) = %.2f\n", sourceAmount);

    return 0;
}