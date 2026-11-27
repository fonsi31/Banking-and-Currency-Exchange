/*******************
Last names: Dimakuta, Cauilan, Marcelino, Baun
Language: C
Paradigm(s): Procedural
*******************/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <math.h>
#include <time.h>

#define CSV_FILE "bank_transactions.csv"
#define MAX_TX 12000
#define MAX_ACC 2000

// Structure from MCO1
typedef struct {
    char accountName[100];
    double balance;
    int isRegistered; // 0 for unregistered, 1 for registered
} BankAccount;

// Structure for each row loaded from bank_transactions.csv
typedef struct {
    char accountName[100];
    long long accountNumber;
    int day, month, year;
    int hour, min, sec;
    char currency[10];
    double amount;
    char type[16];
    double currentBalance;
} Transaction;

// Structure for per-account report calculations (Reports 1, 2, 4)
typedef struct {
    char accountName[100];
    long long accountNumber;
    int firstDay, firstMonth, firstYear;
    double currentBalance;
    double totalDeposits;
    double totalWithdrawals;
    double adb;
    char tier[16];
    double expectedInterest;
    double netCashFlow;
} AccountSummary;

// Structure for daily suspicious activity monitoring (Report 3)
typedef struct {
    char accountName[100];
    long long accountNumber;
    int dateInt;
    char dateStr[16];
    double totalWithdrawals;
    int numDeposits;
    double totalDeposits;
    double velocity;
    int isSuspicious;
} DailyRisk;

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

Transaction txList[MAX_TX];
int txCount = 0;
int dataLoaded = 0;

// Function prototypes (MCO1 + MCO2)
void displayMenu();
void clearInputBuffer();
int askToReturn();
void trimSpaces(char *str);
int equalsIgnoreCase(const char *a, const char *b);
int daysInMonth(int m, int y);
int isValidDate(int d, int m, int y);
int daysSince2000(int d, int m, int y);
double round2(double val);
void formatWithCommas(double val, char *out);
long long findAccountInCsv(const char *query, char *foundName, double *foundBal, long long *highestAccNum);
void appendTransactionToCsv(const char *name, long long accNum, const char *curr, double amt, const char *type, double bal);

void registerAccount();
void deposit();
void withdraw();
void currencyExchange();
void recordExchangeRate();
void showInterestAmount();
void generateReportsMenu();
void loadTransactions();
void generateReports();

int main(void) {
    int choice;

    while (1) {
        displayMenu();
        if (scanf("%d", &choice) != 1) {
            clearInputBuffer();
            printf("Invalid input. Please try again.\n");
            continue;
        }

        switch (choice) {
            case 1:
                registerAccount();
                break;
            case 2:
                deposit();
                break;
            case 3:
                withdraw();
                break;
            case 4:
                currencyExchange();
                break;
            case 5:
                recordExchangeRate();
                break;
            case 6:
                showInterestAmount();
                break;
            case 7:
                generateReportsMenu();
                break;
            case 8:
                printf("\nThank you for using the Banking Application.\n");
                return 0;
            default:
                printf("\nInvalid choice. Please select 1-8.\n\n");
                break;
        }
    }

    return 0;
}

// --- MCO1 Helper Functions ---

void clearInputBuffer() {
    int c;
    while ((c = getchar()) != '\n' && c != EOF) {}
}

int askToReturn() {
    char ans;
    printf("\nBack to the Main Menu (Y/N): ");
    scanf(" %c", &ans);
    return (ans == 'Y' || ans == 'y');
}

void displayMenu() {
    printf("======================================\n");
    printf("Select Transaction:\n");
    printf("[1] Register Account\n");
    printf("[2] Deposit Amount\n");
    printf("[3] Withdraw Amount\n");
    printf("[4] Currency Exchange\n");
    printf("[5] Record Exchange Rates\n");
    printf("[6] Show Interest Amount\n");
    printf("[7] Generate Reports\n");
    printf("[8] Exit\n\n");
    printf("Choice: ");
}

// --- Simple String, Date, and CSV Helpers ---

void trimSpaces(char *str) {
    int start = 0;
    while (str[start] != '\0' && isspace((unsigned char)str[start])) {
        start++;
    }
    int i = 0;
    while (str[start + i] != '\0') {
        str[i] = str[start + i];
        i++;
    }
    str[i] = '\0';
    while (i > 0 && isspace((unsigned char)str[i - 1])) {
        str[--i] = '\0';
    }
}

int equalsIgnoreCase(const char *a, const char *b) {
    int i = 0;
    while (a[i] != '\0' && b[i] != '\0') {
        if (tolower((unsigned char)a[i]) != tolower((unsigned char)b[i])) {
            return 0;
        }
        i++;
    }
    return a[i] == b[i];
}

int daysInMonth(int m, int y) {
    int days[13] = {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31};
    if (m < 1 || m > 12) return 0;
    if (m == 2 && ((y % 4 == 0 && y % 100 != 0) || (y % 400 == 0))) return 29;
    return days[m];
}

int isValidDate(int d, int m, int y) {
    if (y < 1900 || y > 2100 || m < 1 || m > 12) return 0;
    return (d >= 1 && d <= daysInMonth(m, y));
}

int daysSince2000(int d, int m, int y) {
    int total = 0;
    for (int yr = 2000; yr < y; yr++) {
        total += ((yr % 4 == 0 && yr % 100 != 0) || (yr % 400 == 0)) ? 366 : 365;
    }
    for (int mo = 1; mo < m; mo++) {
        total += daysInMonth(mo, y);
    }
    return total + d;
}

double round2(double val) {
    return round(val * 100.0) / 100.0;
}

void formatWithCommas(double val, char *out) {
    char temp[64];
    int k = 0;
    if (val < 0) {
        out[k++] = '-';
        val = -val;
    }
    sprintf(temp, "%.2f", val);
    char *dot = strchr(temp, '.');
    int intDigits = (int)(dot - temp);
    for (int i = 0; i < intDigits; i++) {
        if (i > 0 && (intDigits - i) % 3 == 0) {
            out[k++] = ',';
        }
        out[k++] = temp[i];
    }
    strcpy(out + k, dot);
}

long long findAccountInCsv(const char *query, char *foundName, double *foundBal, long long *highestAccNum) {
    *highestAccNum = 1092000000LL;
    long long matchedAccNum = 0;
    if (foundBal) *foundBal = 0.0;

    FILE *fp = fopen(CSV_FILE, "r");
    if (!fp) return 0;

    char line[512];
    int lineNum = 0;
    while (fgets(line, sizeof(line), fp)) {
        trimSpaces(line);
        if (line[0] == '\0') continue;
        lineNum++;
        if (lineNum == 1) continue; // skip header

        char name[100], date[20], timeStr[20], curr[10], type[20];
        long long accNum;
        double amt, bal;
        if (sscanf(line, "%99[^,],%lld,%19[^,],%19[^,],%9[^,],%lf,%19[^,],%lf",
                   name, &accNum, date, timeStr, curr, &amt, type, &bal) == 8) {
            trimSpaces(name);
            if (accNum > *highestAccNum) {
                *highestAccNum = accNum;
            }
            if (query != NULL && query[0] != '\0') {
                long long qNum = atoll(query);
                if (equalsIgnoreCase(name, query) || (qNum > 0 && accNum == qNum)) {
                    matchedAccNum = accNum;
                    if (foundName) strcpy(foundName, name);
                    if (foundBal) *foundBal = bal;
                }
            }
        }
    }
    fclose(fp);
    return matchedAccNum;
}

void appendTransactionToCsv(const char *name, long long accNum, const char *curr, double amt, const char *type, double bal) {
    FILE *check = fopen(CSV_FILE, "r");
    int writeHeader = 1;
    if (check) {
        if (fgetc(check) != EOF) writeHeader = 0;
        fclose(check);
    }

    FILE *fp = fopen(CSV_FILE, "a");
    if (!fp) {
        printf("Error: Cannot open %s.\n", CSV_FILE);
        return;
    }
    if (writeHeader) {
        fprintf(fp, "account_name,account_number,date,time,currency,amount,type,current_balance\n");
    }

    time_t now = time(NULL);
    struct tm *t = localtime(&now);
    fprintf(fp, "%s,%lld,%02d/%02d/%04d,%02d:%02d:%02d,%s,%.2f,%s,%.2f\n",
            name, accNum,
            t->tm_mday, t->tm_mon + 1, t->tm_year + 1900,
            t->tm_hour, t->tm_min, t->tm_sec,
            curr, amt, type, bal);
    fclose(fp);
}

// --- Core Banking Transactions [1] to [6] ---

void registerAccount() {
    BankAccount newAcc;
    char amtInput[64];
    char extra;
    long long highestAccNum;

    printf("\nRegister Account\n");
    printf("Account Name: ");
    clearInputBuffer();
    if (!fgets(newAcc.accountName, sizeof(newAcc.accountName), stdin)) return;
    trimSpaces(newAcc.accountName);

    if (strchr(newAcc.accountName, ',') != NULL || strchr(newAcc.accountName, '"') != NULL) {
        printf("Name must not contain commas or quotes.\n");
        askToReturn();
        return;
    }
    if (strchr(newAcc.accountName, ' ') == NULL) {
        printf("Enter both a first name and a last name.\n");
        askToReturn();
        return;
    }
    if (findAccountInCsv(newAcc.accountName, NULL, NULL, &highestAccNum) != 0) {
        printf("An account with that name already exists.\n");
        askToReturn();
        return;
    }

    printf("Initial Deposit: ");
    if (!fgets(amtInput, sizeof(amtInput), stdin)) return;
    trimSpaces(amtInput);

    if (sscanf(amtInput, "%lf%c", &newAcc.balance, &extra) != 1) {
        printf("Invalid amount.\n");
        askToReturn();
        return;
    }
    if (newAcc.balance < 5000.00) {
        printf("Amount must be at least 5000.00\n");
        askToReturn();
        return;
    }

    long long newAccNum = highestAccNum + 1;
    newAcc.isRegistered = 1;
    appendTransactionToCsv(newAcc.accountName, newAccNum, "PHP", newAcc.balance, "deposit", newAcc.balance);
    printf("Account created successfully! Account Number: %lld\n", newAccNum);

    askToReturn();
}

void deposit() {
    char query[100], amtInput[64], extra;
    BankAccount acc;
    long long highestAccNum;
    double depositAmount;
    int currChoice;

    printf("\nDeposit Amount\n");
    printf("Account Name or Number: ");
    clearInputBuffer();
    if (!fgets(query, sizeof(query), stdin)) return;
    trimSpaces(query);

    long long accNum = findAccountInCsv(query, acc.accountName, &acc.balance, &highestAccNum);
    if (accNum == 0) {
        printf("Account not found.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc.accountName);
    printf("Current Balance: %.2f\n", acc.balance);
    printf("Currency ([1] PHP, [2] USD, [3] JPY, [4] GBP, [5] EUR, [6] CNY): ");
    if (scanf("%d", &currChoice) != 1 || currChoice < 1 || currChoice > 6) {
        clearInputBuffer();
        printf("Invalid currency.\n");
        askToReturn();
        return;
    }

    printf("Deposit Amount: ");
    clearInputBuffer();
    if (!fgets(amtInput, sizeof(amtInput), stdin)) return;
    trimSpaces(amtInput);

    if (sscanf(amtInput, "%lf%c", &depositAmount, &extra) != 1 || depositAmount <= 0) {
        printf("Invalid deposit amount.\n");
        askToReturn();
        return;
    }

    double phpAmount = round2(depositAmount * exchangeRates[currChoice - 1]);
    if (currChoice != 1) {
        printf("Converted Amount (PHP): %.2f\n", phpAmount);
    }

    acc.balance = round2(acc.balance + phpAmount);
    appendTransactionToCsv(acc.accountName, accNum, "PHP", phpAmount, "deposit", acc.balance);
    printf("Updated Balance: %.2f\n", acc.balance);

    askToReturn();
}

void withdraw() {
    char query[100], amtInput[64], extra;
    BankAccount acc;
    long long highestAccNum;
    double withdrawAmount;

    printf("\nWithdraw Amount\n");
    printf("Account Name or Number: ");
    clearInputBuffer();
    if (!fgets(query, sizeof(query), stdin)) return;
    trimSpaces(query);

    long long accNum = findAccountInCsv(query, acc.accountName, &acc.balance, &highestAccNum);
    if (accNum == 0) {
        printf("Account not found.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc.accountName);
    printf("Current Balance: %.2f\n", acc.balance);
    printf("Currency: PHP\n\n");
    printf("Withdraw Amount: ");
    if (!fgets(amtInput, sizeof(amtInput), stdin)) return;
    trimSpaces(amtInput);

    if (sscanf(amtInput, "%lf%c", &withdrawAmount, &extra) != 1 || withdrawAmount <= 0) {
        printf("Invalid withdraw amount.\n");
        askToReturn();
        return;
    }
    if (withdrawAmount > acc.balance) {
        printf("Insufficient balance. Withdrawal cancelled.\n");
        askToReturn();
        return;
    }

    acc.balance = round2(acc.balance - withdrawAmount);
    appendTransactionToCsv(acc.accountName, accNum, "PHP", withdrawAmount, "withdrawal", acc.balance);
    printf("Updated Balance: %.2f\n", acc.balance);

    askToReturn();
}

// Preserved directly from MCO1
void currencyExchange() {
    double sourceAmount;
    char convertAnother;

    do {
        printf("\nForeign Currency Exchange\n");
        printf("Source Currency Option:\n");
        for (int i = 0; i < 6; i++) {
            printf("[%d] %s\n", i + 1, currencyNames[i]);
        }

        int sourceChoice;
        printf("\nSource Currency: ");
        scanf("%d", &sourceChoice);

        printf("Source Amount: ");
        scanf("%lf", &sourceAmount);

        printf("\nExchanged Currency Options:\n");
        for (int i = 0; i < 6; i++) {
            printf("[%d] %s\n", i + 1, currencyNames[i]);
        }

        int exchChoice;
        printf("\nExchange Currency: ");
        scanf("%d", &exchChoice);

        if (sourceChoice >= 1 && sourceChoice <= 6 && exchChoice >= 1 && exchChoice <= 6) {
            double amountInPHP = sourceAmount;
            if (sourceChoice != 1) {
                amountInPHP = sourceAmount * exchangeRates[sourceChoice - 1];
            }
            double finalAmount = amountInPHP;
            if (exchChoice != 1) {
                finalAmount = amountInPHP / exchangeRates[exchChoice - 1];
            }
            printf("Exchange Amount: %.2f\n\n", finalAmount);
        } else {
            printf("Invalid currency selection.\n\n");
        }

        printf("Convert another currency (Y/N)? ");
        scanf(" %c", &convertAnother);

    } while (convertAnother == 'Y' || convertAnother == 'y');
}

// Preserved directly from MCO1
void recordExchangeRate() {
    int currChoice;
    double newRate;

    printf("\nRecord Exchange Rate\n");
    for (int i = 0; i < 6; i++) {
        printf("[%d] %s\n", i + 1, currencyNames[i]);
    }

    printf("\nSelect Foreign Currency: ");
    scanf("%d", &currChoice);

    if (currChoice == 1) {
        printf("PHP is the base currency and must remain 1.00.\n");
    } else if (currChoice >= 2 && currChoice <= 6) {
        printf("Exchange Rate: ");
        scanf("%lf", &newRate);
        if (newRate > 0) {
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

// Preserved from MCO1 with daily 2-decimal rounding and CSV lookup
void showInterestAmount() {
    char query[100];
    BankAccount acc;
    long long highestAccNum;
    int days;

    printf("\nShow Interest Amount\n");
    printf("Account Name or Number: ");
    clearInputBuffer();
    if (!fgets(query, sizeof(query), stdin)) return;
    trimSpaces(query);

    if (findAccountInCsv(query, acc.accountName, &acc.balance, &highestAccNum) == 0) {
        printf("Account not found.\n");
        askToReturn();
        return;
    }

    printf("Account Name: %s\n", acc.accountName);
    printf("Current Balance: %.2f\n", acc.balance);
    printf("Currency: PHP\n");
    printf("Interest Rate: 5%%\n\n");

    printf("Total Number of Days: ");
    scanf("%d", &days);

    printf("\nDay | Interest | Balance |\n");

    double simulatedBalance = acc.balance;
    for (int i = 1; i <= days; i++) {
        double dailyInterest = round2(simulatedBalance * (0.05 / 365.0));
        simulatedBalance = round2(simulatedBalance + dailyInterest);
        printf("%-3d | %-8.2f | %.2f |\n", i, dailyInterest, simulatedBalance);
    }

    askToReturn();
}

// --- MCO2 Report Generation [7] ---

void generateReportsMenu() {
    int choice;
    while (1) {
        printf("\nGenerate Reports\n");
        printf("[1] Load the file\n");
        printf("[2] Generate Reports\n");
        printf("[3] Back to Main Menu\n");
        printf("Enter choice: ");
        if (scanf("%d", &choice) != 1) {
            clearInputBuffer();
            printf("Invalid input.\n");
            continue;
        }
        if (choice == 1) {
            loadTransactions();
        } else if (choice == 2) {
            if (!dataLoaded) {
                printf("No data loaded yet. Please select [1] Load the file first.\n");
            } else {
                generateReports();
            }
        } else if (choice == 3) {
            return;
        } else {
            printf("Invalid choice.\n");
        }
    }
}

void loadTransactions() {
    txCount = 0;
    dataLoaded = 0;

    FILE *fp = fopen(CSV_FILE, "r");
    if (!fp) {
        printf("Error: %s not found.\n", CSV_FILE);
        printf("0 rows loaded, 0 filtered for PHP currency, 0 error(s) skipped\n");
        dataLoaded = 1;
        return;
    }

    char line[512];
    int rowNum = 0, totalRows = 0, phpRows = 0, errRows = 0;

    while (fgets(line, sizeof(line), fp)) {
        rowNum++;
        trimSpaces(line);
        if (line[0] == '\0') continue; // skip blank lines
        if (rowNum == 1) continue;     // skip header row

        totalRows++;

        // Split row by commas
        char *cols[12];
        int colCount = 0;
        cols[colCount++] = line;
        for (int i = 0; line[i] != '\0' && colCount < 12; i++) {
            if (line[i] == ',') {
                line[i] = '\0';
                cols[colCount++] = &line[i + 1];
            }
        }

        if (colCount != 8) {
            printf("Row %d skipped: expected 8 columns, found %d\n", rowNum, colCount);
            errRows++;
            continue;
        }

        for (int i = 0; i < 8; i++) trimSpaces(cols[i]);

        int d, m, y, hh, mm, ss;
        long long accNum;
        double amt, bal;
        char extra;

        int okAcc = (sscanf(cols[1], "%lld%c", &accNum, &extra) == 1 && accNum > 0);
        int okDate = (sscanf(cols[2], "%d/%d/%d%c", &d, &m, &y, &extra) == 3) && isValidDate(d, m, y);
        int okTime = (sscanf(cols[3], "%d:%d:%d%c", &hh, &mm, &ss, &extra) == 3) &&
                     (hh >= 0 && hh <= 23 && mm >= 0 && mm <= 59 && ss >= 0 && ss <= 59);
        int okAmt = (sscanf(cols[5], "%lf%c", &amt, &extra) == 1 && amt > 0.0);
        int okBal = (sscanf(cols[7], "%lf%c", &bal, &extra) == 1 && bal >= 0.0);

        for (int i = 0; cols[4][i]; i++) cols[4][i] = (char)toupper((unsigned char)cols[4][i]);
        for (int i = 0; cols[6][i]; i++) cols[6][i] = (char)tolower((unsigned char)cols[6][i]);

        int okCurr = (strcmp(cols[4], "PHP") == 0 || strcmp(cols[4], "USD") == 0 ||
                      strcmp(cols[4], "JPY") == 0 || strcmp(cols[4], "GBP") == 0 ||
                      strcmp(cols[4], "EUR") == 0 || strcmp(cols[4], "CNY") == 0);
        int okType = (strcmp(cols[6], "deposit") == 0 || strcmp(cols[6], "withdrawal") == 0);

        if (cols[0][0] == '\0' || !okAcc || !okDate || !okTime || !okCurr || !okAmt || !okType || !okBal) {
            printf("Row %d skipped: invalid or missing field(s)\n", rowNum);
            errRows++;
            continue;
        }

        if (strcmp(cols[4], "PHP") == 0 && phpRows < MAX_TX) {
            strcpy(txList[phpRows].accountName, cols[0]);
            txList[phpRows].accountNumber = accNum;
            txList[phpRows].day = d;
            txList[phpRows].month = m;
            txList[phpRows].year = y;
            txList[phpRows].hour = hh;
            txList[phpRows].min = mm;
            txList[phpRows].sec = ss;
            strcpy(txList[phpRows].currency, cols[4]);
            txList[phpRows].amount = amt;
            strcpy(txList[phpRows].type, cols[6]);
            txList[phpRows].currentBalance = bal;
            phpRows++;
        }
    }
    fclose(fp);

    txCount = phpRows;
    dataLoaded = 1;
    printf("%d rows loaded, %d filtered for PHP currency, %d error(s) skipped\n", totalRows, phpRows, errRows);
}

void generateReports() {
    printf("Generating reports...\n");

    // 1. Sort transactions chronologically (Selection Sort)
    for (int i = 0; i < txCount - 1; i++) {
        int minIdx = i;
        int dateMin = txList[minIdx].year * 10000 + txList[minIdx].month * 100 + txList[minIdx].day;
        int timeMin = txList[minIdx].hour * 3600 + txList[minIdx].min * 60 + txList[minIdx].sec;
        for (int j = i + 1; j < txCount; j++) {
            int dateJ = txList[j].year * 10000 + txList[j].month * 100 + txList[j].day;
            int timeJ = txList[j].hour * 3600 + txList[j].min * 60 + txList[j].sec;
            if (dateJ < dateMin || (dateJ == dateMin && timeJ < timeMin)) {
                minIdx = j;
                dateMin = dateJ;
                timeMin = timeJ;
            }
        }
        if (minIdx != i) {
            Transaction temp = txList[i];
            txList[i] = txList[minIdx];
            txList[minIdx] = temp;
        }
    }

    // 2. Find latest transaction date (reference date) and prior calendar month
    int latD = 1, latM = 1, latY = 2026;
    if (txCount > 0) {
        latD = txList[txCount - 1].day;
        latM = txList[txCount - 1].month;
        latY = txList[txCount - 1].year;
    }
    int prevM = (latM == 1) ? 12 : latM - 1;
    int prevY = (latM == 1) ? latY - 1 : latY;
    int totalDaysInPrevMonth = daysInMonth(prevM, prevY);

    // 3. Group transactions by unique account
    AccountSummary accs[MAX_ACC];
    int accCount = 0;

    for (int i = 0; i < txCount; i++) {
        int idx = -1;
        for (int j = 0; j < accCount; j++) {
            if (accs[j].accountNumber == txList[i].accountNumber) {
                idx = j;
                break;
            }
        }
        if (idx == -1 && accCount < MAX_ACC) {
            idx = accCount++;
            strcpy(accs[idx].accountName, txList[i].accountName);
            accs[idx].accountNumber = txList[i].accountNumber;
            accs[idx].firstDay = txList[i].day;
            accs[idx].firstMonth = txList[i].month;
            accs[idx].firstYear = txList[i].year;
            accs[idx].totalDeposits = 0.0;
            accs[idx].totalWithdrawals = 0.0;
        }
        if (idx != -1) {
            accs[idx].currentBalance = txList[i].currentBalance;
            if (strcmp(txList[i].type, "deposit") == 0) {
                accs[idx].totalDeposits += txList[i].amount;
            } else {
                accs[idx].totalWithdrawals += txList[i].amount;
            }
        }
    }

    // 4. Compute Report 1 (ADB & Tier) and Report 2 (30-day Interest) per account
    for (int j = 0; j < accCount; j++) {
        double eod[32] = {0};
        double runningBal = 0.0;
        int isOpened = 0;
        int lastDay = 0;

        for (int i = 0; i < txCount; i++) {
            if (txList[i].accountNumber != accs[j].accountNumber) continue;

            if (txList[i].year < prevY || (txList[i].year == prevY && txList[i].month < prevM)) {
                runningBal = txList[i].currentBalance;
                isOpened = 1;
            } else if (txList[i].year == prevY && txList[i].month == prevM) {
                for (int d = lastDay + 1; d < txList[i].day; d++) {
                    eod[d] = isOpened ? runningBal : 0.0;
                }
                runningBal = txList[i].currentBalance;
                isOpened = 1;
                eod[txList[i].day] = runningBal;
                lastDay = txList[i].day;
            }
        }
        for (int d = lastDay + 1; d <= totalDaysInPrevMonth; d++) {
            eod[d] = isOpened ? runningBal : 0.0;
        }

        double sumEod = 0.0;
        for (int d = 1; d <= totalDaysInPrevMonth; d++) sumEod += eod[d];
        accs[j].adb = (totalDaysInPrevMonth > 0) ? round2(sumEod / totalDaysInPrevMonth) : 0.0;

        if (accs[j].adb >= 1000000.00) strcpy(accs[j].tier, "Platinum");
        else if (accs[j].adb >= 500000.00) strcpy(accs[j].tier, "Gold");
        else if (accs[j].adb >= 100000.00) strcpy(accs[j].tier, "Silver");
        else strcpy(accs[j].tier, "None");

        double simBal = accs[j].currentBalance;
        for (int d = 1; d <= 30; d++) {
            double dailyIntr = round2(simBal * (0.05 / 365.0));
            simBal = round2(simBal + dailyIntr);
        }
        accs[j].expectedInterest = round2(simBal - accs[j].currentBalance);
        accs[j].netCashFlow = round2(accs[j].totalDeposits - accs[j].totalWithdrawals);
    }

    char num1[64], num2[64], num3[64], num4[64];

    printf("Outputs saved to individual files...\n");

    // --- Export Report 1 (sorted descending by ADB) ---
    for (int i = 0; i < accCount - 1; i++) {
        for (int j = i + 1; j < accCount; j++) {
            if (accs[j].adb > accs[i].adb) {
                AccountSummary temp = accs[i];
                accs[i] = accs[j];
                accs[j] = temp;
            }
        }
    }
    FILE *f1 = fopen("report1_banking_privilege_tiers.csv", "w");
    fprintf(f1, "AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivilegeTier\n");
    for (int j = 0; j < accCount; j++) {
        if (accs[j].adb >= 100000.00) {
            int age = daysSince2000(latD, latM, latY) -
                      daysSince2000(accs[j].firstDay, accs[j].firstMonth, accs[j].firstYear);
            formatWithCommas(accs[j].currentBalance, num1);
            formatWithCommas(accs[j].adb, num2);
            fprintf(f1, "%s,%d,\"%s\",\"%s\",%s\n", accs[j].accountName, age, num1, num2, accs[j].tier);
        }
    }
    fclose(f1);

    printf("\nReport 1: Banking Privilege Tiers\n");
    printf("AccountName | AccountAge | CurrentBalance | AverageDailyBalance | PrivilegeTier\n");
    int shown1 = 0;
    for (int j = 0; j < accCount && shown1 < 2; j++) {
        if (accs[j].adb >= 100000.00) {
            int age = daysSince2000(latD, latM, latY) -
                      daysSince2000(accs[j].firstDay, accs[j].firstMonth, accs[j].firstYear);
            formatWithCommas(accs[j].currentBalance, num1);
            formatWithCommas(accs[j].adb, num2);
            printf("%s | %d | %s | %s | %s\n", accs[j].accountName, age, num1, num2, accs[j].tier);
            shown1++;
        }
    }
    printf("(Full table exported to report1_banking_privilege_tiers.csv)\n");

    // --- Export Report 2 (sorted descending by ExpectedInterest) ---
    for (int i = 0; i < accCount - 1; i++) {
        for (int j = i + 1; j < accCount; j++) {
            if (accs[j].expectedInterest > accs[i].expectedInterest) {
                AccountSummary temp = accs[i];
                accs[i] = accs[j];
                accs[j] = temp;
            }
        }
    }
    FILE *f2 = fopen("report2_interest_expense_projection.csv", "w");
    fprintf(f2, "AccountName,CurrentBalance,ExpectedInterest\n");
    double totalInterestExpense = 0.0;
    for (int j = 0; j < accCount; j++) {
        if (accs[j].currentBalance >= 10000.00) {
            totalInterestExpense += accs[j].expectedInterest;
            formatWithCommas(accs[j].currentBalance, num1);
            formatWithCommas(accs[j].expectedInterest, num2);
            fprintf(f2, "%s,\"%s\",\"%s\"\n", accs[j].accountName, num1, num2);
        }
    }
    fclose(f2);

    printf("\nReport 2: Interest Expense Projection\n");
    printf("AccountName | CurrentBalance | ExpectedInterest\n");
    int shown2 = 0;
    for (int j = 0; j < accCount && shown2 < 2; j++) {
        if (accs[j].currentBalance >= 10000.00) {
            formatWithCommas(accs[j].currentBalance, num1);
            formatWithCommas(accs[j].expectedInterest, num2);
            printf("%s | %s | %s\n", accs[j].accountName, num1, num2);
            shown2++;
        }
    }
    printf("(Full table exported to report2_interest_expense_projection.csv)\n");

    // --- Build & Export Report 3 (Suspicious Client Behavior) ---
    static DailyRisk r3[MAX_TX];
    int r3Count = 0;
    for (int i = 0; i < txCount; i++) {
        int dInt = txList[i].year * 10000 + txList[i].month * 100 + txList[i].day;
        int idx = -1;
        for (int j = 0; j < r3Count; j++) {
            if (r3[j].accountNumber == txList[i].accountNumber && r3[j].dateInt == dInt) {
                idx = j;
                break;
            }
        }
        if (idx == -1) {
            idx = r3Count++;
            strcpy(r3[idx].accountName, txList[i].accountName);
            r3[idx].accountNumber = txList[i].accountNumber;
            r3[idx].dateInt = dInt;
            sprintf(r3[idx].dateStr, "%02d/%02d/%04d", txList[i].day, txList[i].month, txList[i].year);
            r3[idx].totalWithdrawals = 0.0;
            r3[idx].numDeposits = 0;
            r3[idx].totalDeposits = 0.0;
        }
        if (strcmp(txList[i].type, "withdrawal") == 0) {
            r3[idx].totalWithdrawals += txList[i].amount;
        } else {
            r3[idx].numDeposits++;
            r3[idx].totalDeposits += txList[i].amount;
        }
    }

    for (int j = 0; j < r3Count; j++) {
        double rawVel = (r3[j].totalDeposits > 0.0) ? (r3[j].totalWithdrawals / r3[j].totalDeposits) * 100.0 : 0.0;
        r3[j].velocity = round2(rawVel);
        r3[j].isSuspicious = (r3[j].totalWithdrawals >= 500000.00 - 1e-9 || rawVel >= 90.0 - 1e-9) ? 1 : 0;
    }

    // Sort Report 3: Date ascending, then Suspicious before Normal (Selection Sort)
    for (int i = 0; i < r3Count - 1; i++) {
        int best = i;
        for (int j = i + 1; j < r3Count; j++) {
            if (r3[j].dateInt < r3[best].dateInt ||
               (r3[j].dateInt == r3[best].dateInt && r3[j].isSuspicious > r3[best].isSuspicious)) {
                best = j;
            }
        }
        if (best != i) {
            DailyRisk temp = r3[i];
            r3[i] = r3[best];
            r3[best] = temp;
        }
    }

    FILE *f3 = fopen("report3_suspicious_client_behavior.csv", "w");
    fprintf(f3, "AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel\n");
    for (int j = 0; j < r3Count; j++) {
        formatWithCommas(r3[j].totalWithdrawals, num1);
        formatWithCommas(r3[j].totalDeposits, num2);
        formatWithCommas(r3[j].velocity, num3);
        fprintf(f3, "%s,%s,\"%s\",%d,\"%s\",\"%s\",%s\n",
                r3[j].accountName, r3[j].dateStr, num1, r3[j].numDeposits, num2, num3,
                r3[j].isSuspicious ? "Suspicious" : "Normal");
    }
    fclose(f3);

    printf("\nReport 3: Suspicious Client Behavior\n");
    printf("AccountName | Date | TotalWithdrawalAmounts | NumberOfDeposits | TotalDepositAmounts | TurnoverVelocity | RiskFlagLevel\n");
    for (int j = 0; j < r3Count && j < 2; j++) {
        formatWithCommas(r3[j].totalWithdrawals, num1);
        formatWithCommas(r3[j].totalDeposits, num2);
        formatWithCommas(r3[j].velocity, num3);
        printf("%s | %s | %s | %d | %s | %s | %s\n",
               r3[j].accountName, r3[j].dateStr, num1, r3[j].numDeposits, num2, num3,
               r3[j].isSuspicious ? "Suspicious" : "Normal");
    }
    printf("(Full table exported to report3_suspicious_client_behavior.csv)\n");

    // --- Export Report 4: Custom Report (Net Cash Flow Summary, sorted descending) ---
    for (int i = 0; i < accCount - 1; i++) {
        for (int j = i + 1; j < accCount; j++) {
            if (accs[j].netCashFlow > accs[i].netCashFlow) {
                AccountSummary temp = accs[i];
                accs[i] = accs[j];
                accs[j] = temp;
            }
        }
    }
    FILE *f4 = fopen("report4_net_cash_flow_summary.csv", "w");
    fprintf(f4, "AccountName,TotalDeposits,TotalWithdrawals,NetCashFlow,CurrentBalance\n");
    for (int j = 0; j < accCount; j++) {
        formatWithCommas(accs[j].totalDeposits, num1);
        formatWithCommas(accs[j].totalWithdrawals, num2);
        formatWithCommas(accs[j].netCashFlow, num3);
        formatWithCommas(accs[j].currentBalance, num4);
        fprintf(f4, "%s,\"%s\",\"%s\",\"%s\",\"%s\"\n", accs[j].accountName, num1, num2, num3, num4);
    }
    fclose(f4);

    // --- Export summary.json ---
    double sumDep = 0.0, sumWith = 0.0, sumBal = 0.0;
    for (int j = 0; j < accCount; j++) {
        sumDep += accs[j].totalDeposits;
        sumWith += accs[j].totalWithdrawals;
        sumBal += accs[j].currentBalance;
    }
    FILE *fj = fopen("summary.json", "w");
    fprintf(fj, "{\n"
                "  \"total_accounts\": %d,\n"
                "  \"total_deposits\": %.2f,\n"
                "  \"total_withdrawals\": %.2f,\n"
                "  \"total_balance\": %.2f,\n"
                "  \"total_interest_expense\": %.2f\n"
                "}\n",
            accCount, round2(sumDep), round2(sumWith), round2(sumBal), round2(totalInterestExpense));
    fclose(fj);

    printf("\nSummary State (summary.json):\n");
    printf("{\"total_accounts\": %d, \"total_deposits\": %.2f, \"total_withdrawals\": %.2f, \"total_balance\": %.2f, \"total_interest_expense\": %.2f}\n",
           accCount, round2(sumDep), round2(sumWith), round2(sumBal), round2(totalInterestExpense));
}