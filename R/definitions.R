path <- "../bank_transactions.csv"

# Displays the main menu
main_menu <- function() {
    cat(
        "Select Transaction:\n",
        "[1] Register Account\n",
        "[2] Deposit Amount\n",
        "[3] Withdraw Amount\n",
        "[4] Currency Exchange\n",
        "[5] Record Exchange Rates\n",
        "[6] Show Interest Amount\n",
        "[7] Generate Reports\n",
        "[8] Exit\n",
        sep = ""
    )
}

# reads and loads the account records from the database
#
# Returns: The registered accounts stored in a data frame (table)
load_transactions <- function() {
    transactions <- read.csv(path, colClasses = "character", strip.white = TRUE)
    rows_loaded <- nrow(transactions)

    transactions$amount <- suppressWarnings(as.numeric(transactions$amount))
    transactions$current_balance <- suppressWarnings(as.numeric(transactions$current_balance))

    date_and_time <- paste(transactions$date, transactions$time)
    transactions$datetime <- as.POSIXct(date_and_time, format = "%d/%m/%Y %H:%M:%S", tz = "UTC")
    transactions$date <- as.Date(transactions$date, format = "%d/%m/%Y")

    is_bad <-   is.na(transactions$datetime) |
                is.na(transactions$amount) |
                is.na(transactions$current_balance) |
                !grepl("^[0-9]{10}$", transactions$account_number) |
                !(transactions$type %in% c("deposit", "withdrawal"))

    invalid_rows <- sum(is_bad)
    transactions <- transactions[!is_bad, ]

    transactions <- transactions[transactions$currency %in% "PHP", ]

    cat("Processing dataset... (",
        format(rows_loaded, big.mark = ","), " rows loaded, ",
        format(nrow(transactions), big.mark = ","), " filtered for PHP currency, ",
        format(invalid_rows, big.mark = ","), " invalid rows removed)\n",
        sep = "")

    transactions[order(transactions$datetime), ]
}

# Updates the transactions database
#
# Parameters:
# - transactions: data frame (1 row corresponds to one transaction)
save_transactions <- function(transactions, account_name, account_number,
                        amount, type, new_balance) {
    now <- Sys.time()

    file_row <- data.frame(
        account_name = account_name,
        account_number = account_number,
        date = format(now, "%d/%m/%Y"),
        time = format(now, "%H:%M:%S"),
        currency = "PHP",
        amount = sprintf("%.2f", amount),
        type = type,
        current_balance = sprintf("%.2f", new_balance)
    )

    #append it to the end of the file
    write.table(file_row, path, sep = ",", append = TRUE,
                col.names = FALSE, row.names = FALSE, quote = FALSE)

    #add the same row to the data frame in memory (program transactions)
    memory_row <- data.frame(
        account_name = account_name,
        account_number = account_number,
        date = as.Date(file_row$date, format = "%d/%m/%Y"),
        time = file_row$time,
        currency = "PHP",
        amount = round(amount, 2),
        type = type,
        current_balance = round(new_balance, 2),
        datetime = as.POSIXct(paste(file_row$date, file_row$time),
                 format = "%d/%m/%Y %H:%M:%S", tz = "UTC")
    )

    rbind(transactions, memory_row)
}

# Displays the currency exchange records against php
#
# Parameters:
# - exchange_rates: Numeric vector containing exchange rates
#   relative to PHP
display_currency_exchange <- function(exchange_rates) {
    n <- length(exchange_rates)
    cat("PHP Exchange Rates Table\n")
    cat("-----------------------------------------------------------\n")
    cat(sprintf("%-30s| %-5.2f %-4s| inv. %-5.2f %-4s\n", names(exchange_rates)[1],
    1, "PHP", 1, "PHP"))
    for (i in 2:n){
        inv <- convert_currency(1, i, 1, exchange_rates)
        cat(sprintf("%-30s| %-10.3f| %-10.3f\n", names(exchange_rates)[i],
        exchange_rates[[i]], inv))
    }
    cat("\n")
}
  
# Converts between any supported currencies
#
# Parameters:
# - amount: Amount in source currency
# - from: Index of the source currency in the exchange rates
# - to: Index of the target currency in the exchange rates
# - exchange_rates: Numeric vector containing exchange rates
#   relative to PHP
#
# Returns: The amount expressed in the target currency
convert_currency <- function(amount, from, to, exchange_rates){
    #convert amount to php
    #if from == php (index 1) this stays unchanged
    to_php <- amount / exchange_rates[[from]]
    
    if (to != 1) {
        return(to_php * exchange_rates[[to]])
    }

    return(to_php)
}

is_valid_name <- function(name){
    grepl("^[A-Za-z][A-Za-z.'-]*( [A-Za-z][A-Za-z.'-]*)+$", name)
}

# Generates an account number for a new registration
next_account_number <- function(transactions) {
    numbers <- as.numeric(transactions$account_number)

    if (length(numbers) == 0) {
        return("1092000001")
    }
    
    sprintf("%.0f", max(numbers) + 1)
}

read_amount <- function(prompt, minimum = 0.01) {
    repeat {
        input <- trimws(readline(prompt))

        if (grepl("^[0-9]+(\\.[0-9]{1,2})?$", input)) {
            amount <- as.numeric(input)
            if (amount >= minimum) return(amount)
        }

        cat(sprintf("Invalid amount. Enter a number of at least %s with up to 2 decimals.\n",
                formatC(minimum, format = "f", digits = 2, big.mark = ",")))
    }
}

# Registers an account
#
# Parameters:
# - transactions: rows of transactions
#
# Returns: The new updated list of accounts
register <- function(transactions) {
    while (TRUE) {
        cat("\014")
        cat("Register Account Name\n")
        account_name <- trimws(readline("Enter Account Name (First Last): "))

        if (!is_valid_name(account_name)) {
            cat("Invalid account name\n")

            choice <- readline("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        deposit <- read_amount("Initial Deposit: ")

        if (deposit < 5000) {
            cat("Initial Deposit must not be lower than 5000\n")

            choice <- readline("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        acct_num <- next_account_number(transactions)

        transactions <- save_transactions(transactions, account_name, acct_num,
                    deposit, "deposit", deposit)

        cat("Account Created Successfuly!\n")
        cat("Hi", account_name, "!\n")
        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y") {
            break
        }
    }
    transactions
}

account_exists <- function(transactions, accno) {
    grepl("^[0-9]{10}$", accno) && accno %in% transactions$account_number
}

get_current_balance <- function(transactions, accno) {
    account_rows <- transactions[transactions$account_number == accno, ]
    tail(account_rows$current_balance, 1)
}

get_account_name <- function(transactions, accno) {
    account_rows <- transactions[transactions$account_number == accno, ]
    tail(account_rows$account_name, 1)
}

# Deposits an amount to an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
deposit <- function(transactions) {
    while (TRUE) {
        cat("\014")
        cat("Deposit Amount\n")
        accno <- readline("Account Number: ")

        #checks if account number doesn't exists
        if (!account_exists(transactions, accno)) {
            cat("Account doesn't exists!")

            choice <- readline("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        current_balance <- get_current_balance(transactions, accno)

        cat(sprintf("Current Balance: %.2f\n", current_balance))
        cat("Currency: PHP\n")
        cat("\n")

        deposit_amt <- read_amount("Enter amount to deposit: ")
        account_name <- get_account_name(transactions, accno)
        new_balance <- current_balance + deposit_amt

        transactions <- save_transactions(transactions, account_name, accno, 
                    deposit_amt, "deposit", new_balance)
        
        cat(sprintf("Updated Balance: %.2f\n", new_balance))
        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y") {
            break
        }
    }

    transactions
}

# Withdraws an amount from an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
withdraw <- function(transactions){
    while(TRUE){
        cat("\014")
        cat("Withdraw Amount\n")
        accno <- readline("Account Number: ")

        #checks if account number doesn't exists
        if (!account_exists(transactions, accno)) {
            cat("Account doesn't exists!")

            choice <- readline("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        current_balance <- get_current_balance(transactions, accno)

        cat(sprintf("Current Balance: %.2f\n", current_balance))
        cat("Currency: PHP\n")
        cat("\n")

        withdraw_amt <- read_amount()

        #checks if current balance can handle withdraw amount
        if (withdraw_amt > current_balance) {
            cat("Insufficient Balance!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        account_name <- get_account_name(transactions, accno)
        new_balance <- current_balance - withdraw_amt

        transactions <- save_transactions(transactions, account_name, accno, 
                    withdraw_amt, "deposit", new_balance)

        cat(sprintf("Updated Balance: %.2f", new_balance))
        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y"){
            break
        }
    }

    transactions
}

# Allows a user to view the equivalent amount of one currency to another
#
# Parameters:
# - exchange_rates: Numeric vector containing exchange rates
#   relative to PHP
currency_exchange <- function(exchange_rates) {
    while(TRUE) {
        cat("\014")
        display_currency_exchange(exchange_rates)
        cat("Foreign Currency Exchange\n")

        i <- 1
        for (currency in names(exchange_rates)){
            cat(sprintf("[%d] %s\n", i, currency))
            i <- i + 1
        }

        from <- readline("FROM: ")
        source_amt <- as.numeric(readline("Source Amount: "))
        to <- readline("TO: ")
        
        num_of_currs <- length(exchange_rates)

        if (!grepl("^-?[0-9]+$", from) ||
        !grepl("^-?[0-9]+$", to)
        || as.integer(from) < 1 || as.integer(from) > num_of_currs ||
        as.integer(to) < 1 || as.integer(to) > num_of_currs ||
        is.na(source_amt)) {
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        from <- as.integer(from)
        to <- as.integer(to)
        converted_amt <- convert_currency(source_amt, from, to, exchange_rates)
        cat(sprintf("Exchange Amount: %.3f\n", converted_amt))
        cat("\n")
        choice <- readline("Convert another currency (Y/N)? ... ")
        if (tolower(choice) == "n") {
            break
        }
        next
    }
}

# Manually sets the exchange rate of
# any supported currency relative to PHP
#
# Parameters:
# - exchange_rates: Numeric vector containing exchange rates
#   relative to PHP
#
# Returns: The new updated exchange rates
record_exchange_rate <- function(exchange_rates){
    while (TRUE) {
        cat("\014")
        display_currency_exchange(exchange_rates)
        cat("Record Exchange Rate\n")
        cat("Note: this is measured against PHP\n")
        cat("\n")

        num_of_currs <- length(exchange_rates)
        for (i in 2:num_of_currs){
            cat(sprintf("[%d] %s\n", i - 1, names(exchange_rates)[i]))
        }
        cat("\n") 

        foreign_currency <- readline("Select Foreign Currency: ")
        if (!grepl("^-?[0-9]+$", foreign_currency) || as.integer(foreign_currency) < 1 ||
        as.integer(foreign_currency) > num_of_currs - 1) {
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        new_exchange_rate <- as.numeric(readline("Set New Exchange Rate: "))
        if (is.na(new_exchange_rate)){
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        i = as.integer(foreign_currency) + 1 #index of the selected currency
        exchange_rates[i] <- new_exchange_rate

        cat("New exchange rate set for", names(exchange_rates)[i], "\n")

        choice <- readline("Go Back to the Main Menu (Y/N)? ... ")
        if (tolower(choice) == "y"){
            break
        }
    }
    
    return(exchange_rates)
}

# Displays projected balances and interest for a user-specified
# number of days
#
# Parameters:
# - transactions: rows of all transactions made
show_interest_amt <- function(transactions) {
    while (TRUE) {
        cat("\014")
        cat("Show Interest Amount\n")

        accno <- readline("Account Number: ")

        #checks if an account exists in the registered accounts
        if (!account_exists(transactions, accno)) {
            cat("Account not found!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        interest_rate <- 0.05

        current_bal <- get_current_balance(transactions, accno)
        cat(sprintf("Current Balance: %.2f\n", current_bal))
        cat("Currency: PHP\n")
        cat("Interest Rate: 5%\n")
        cat("\n")
        days <- readline("Total Number of Days: ")
        
        #input validation for number of days
        if (!grepl("^-?[0-9]+$", days)) {
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }
        days <- as.integer(days)
        
        #input validation for number of days
        if (days < 1) {
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        cat("\n")

        cat(sprintf("%-4s| %-9s| %-10s|\n", "Day", "Interest", "Balance"))
        for (i in 1:days){
            daily_interest <- current_bal * (interest_rate / 365)
            current_bal <- current_bal + daily_interest
            cat(sprintf("%-4d| %-9.2f| %-10.2f|\n", i, daily_interest, current_bal))
        }

        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")
        if (tolower(choice) == "y") {
            break
        }
    }
}

is_valid_int_choice <- function(input, lb, ub) {
    if (!grepl("^-?[0-9]{1,9}$", input)) return(FALSE)

    input <- as.integer(input)

    if (input < lb || input > ub) {
        return(FALSE)
    }

    TRUE
}

# Generate Reports submenu
#
# Parameters:
# - transactions: data frame (1 row corresponds to one transaction)
#
# Returns: transactions (reloaded if the user chose "Load the file")
generate_reports <- function(transactions) {
    while (TRUE) {
        cat("\014")
        cat("Generate Reports\n")
        cat("\n")
        cat("[1] Load the file\n",
            "[2] Generate Reports\n",
            "[3] Back\n",
            sep = "")

        choice <- readline("Enter choice: ")

        #checks if the user inputs a valid int and if it is within range
        if (!is_valid_int_choice(choice, 1, 3)) {
            cat("Invalid Input!\n")
            readline("Press Enter to continue...")
            next
        }

        choice <- as.integer(choice)

        if (choice == 3) break

        if (choice == 1) {
            transactions <- load_transactions()
            readline("Press Enter to continue...")
            next
        }

        # choice == 2
        cat("\nGenerating reports...\n")
        cat("Outputs saved to individual files...\n\n")

        report1 <- build_report1(transactions)
        write_report(report1, "report1_banking_privilege_tiers.csv",
                     "Report 1: Banking Privilege Tiers",
                     c("CurrentBalance", "AverageDailyBalance"))

        report2 <- build_report2(transactions)
        write_report(report2, "report2_interest_expense_projection.csv",
                     "Report 2: Interest Expense Projection",
                     c("CurrentBalance", "ExpectedInterest"))

        report3 <- build_report3(transactions)
        write_report(report3, "report3_suspicious_client_behavior.csv",
                     "Report 3: Suspicious Client Behavior",
                     c("TotalWithdrawalAmounts", "TotalDepositAmounts", "TurnoverVelocity"))

        report4 <- build_report4(transactions)
        write_report(report4, "report4_client_activity_ranking.csv",
                     "Report 4: Client Activity Ranking",
                     c("TotalAmountMoved", "AverageTransactionSize"))

        write_summary(transactions, report2, "summary.json")

        readline("Press Enter to continue...")
    }

    transactions
}

# Formats money: 1234567.891 -> "1,234,567.89" (NA -> "N/A")
format_money <- function(x) {
    formatted <- formatC(round(x, 2), format = "f", digits = 2, big.mark = ",")
    formatted[is.na(x)] <- "N/A"
    formatted
}

# Writes a report to a CSV file and shows a 2-row preview
#
# Parameters:
# - report: data frame with real numbers (not yet formatted)
# - filename: name of the CSV file to create
# - title: title shown on screen
# - money_columns: names of the columns to format as "1,234,567.89"
write_report <- function(report, filename, title, money_columns) {
    output <- report  

    for (column in money_columns) {
        output[[column]] <- format_money(output[[column]])
    }

    # dates are written in the same format as the input CSV
    for (column in names(output)) {
        if (inherits(output[[column]], "Date")) {
            output[[column]] <- format(output[[column]], "%d/%m/%Y")
        }
    }

    # every field becomes text so every field is quoted the same way
    output[] <- lapply(output, as.character)

    write.csv(output, filename, row.names = FALSE)

    cat(title, "\n")
    print(head(output, 2), row.names = FALSE)
    cat(sprintf("\n(Full table exported to %s)\n\n", filename))
}

# Writes summary.json with totals across the reports
#
# Parameters:
# - transactions: data frame of all transactions
# - report2: Interest Expense Projection report (for total interest expense)
# - filename: name of the JSON file to create
write_summary <- function(transactions, report2, filename) {
    accounts <- unique(transactions$account_number)

    current_balances <- numeric(length(accounts))
    for (i in seq_along(accounts)) {
        current_balances[i] <- get_current_balance(transactions, accounts[i])
    }

    is_deposit <- transactions$type == "deposit"
    is_withdrawal <- transactions$type == "withdrawal"

    json <- sprintf(paste0(
        "{\"total_accounts\": %d, \"total_deposits\": %.2f, ",
        "\"total_withdrawals\": %.2f, \"total_balance\": %.2f, ",
        "\"total_interest_expense\": %.2f}"),
        length(accounts),
        sum(transactions$amount[is_deposit]),
        sum(transactions$amount[is_withdrawal]),
        sum(current_balances),
        sum(report2$ExpectedInterest))

    writeLines(json, filename)

    cat("Summary State (", filename, "):\n", json, "\n", sep = "")
}

# Report 1: Banking Privilege Tiers
#
# Parameters:
# - transactions: data frame of all transactions
# - reference_date: date the report is computed relative to
#   (default: latest transaction date, so outputs are reproducible)
#
# Returns: data frame sorted descending by AverageDailyBalance
build_report1 <- function(transactions, reference_date = max(transactions$date)) {
    #"last month" = the full calendar month before the reference date's month
    first_of_this_month <- as.Date(format(reference_date, "%Y-%m-01"))
    month_end <- first_of_this_month - 1
    month_start <- as.Date(format(month_end, "%Y-%m-01"))
    days <- seq(month_start, month_end, by = "day") #vector of every date from last month

    accounts <- unique(transactions$account_number) #vector of unique account numbers

    rows <- list()

    for (acct in accounts) {
        #stores all the transactions made by an account
        acct_rows <- transactions[transactions$account_number == acct, ]

        end_of_day <- numeric(length(days)) #end of day balances

        for (i in seq_along(days)) {
            #latest balance on or before this day (carries over on days with no transactions)
            up_to_day <- acct_rows$current_balance[acct_rows$date <= days[i]]
            end_of_day[i] <- if (length(up_to_day) == 0) 0 else tail(up_to_day, 1)
        }

        rows[[acct]] <- data.frame(
            AccountNumber = acct,
            AccountName = acct_rows$account_name[1],
            AccountAge = as.integer(reference_date - acct_rows$date[1]),
            CurrentBalance = tail(acct_rows$current_balance, 1),
            AverageDailyBalance = sum(end_of_day) / length(days)
        )
    }

    report <- do.call(rbind, rows)
    report <- report[report$AverageDailyBalance >= 100000, ]
    report$PrivilegeTier <- ifelse(report$AverageDailyBalance < 500000, "Silver",
                            ifelse(report$AverageDailyBalance < 1000000, "Gold", "Platinum"))

    report <- report[order(-report$AverageDailyBalance, report$AccountNumber), ]
    report$AccountNumber <- NULL #only used for tie-breaking
    rownames(report) <- NULL
    report
}

# Report 2: Interest Expense Projection
# Simple interest over 30 days at 5% per annum, based on the latest current balance
#
# Returns: data frame sorted descending by ExpectedInterest
build_report2 <- function(transactions) {
    accounts <- unique(transactions$account_number)

    rows <- list() #each rows of the report will be stored here

    for (acct in accounts) {
        acct_rows <- transactions[transactions$account_number == acct, ]
        current_balance <- tail(acct_rows$current_balance, 1)

        #daily interest = balance x (0.05 / 365), for 30 days
        expected_interest <- current_balance * (0.05 / 365) * 30

        rows[[acct]] <- data.frame(
            AccountNumber = acct,
            AccountName = acct_rows$account_name[1],
            CurrentBalance = current_balance,
            ExpectedInterest = expected_interest
        )
    }

    report <- do.call(rbind, rows)
    report <- report[report$CurrentBalance >= 10000, ]
    report <- report[order(-report$ExpectedInterest, report$AccountNumber), ]
    report$AccountNumber <- NULL
    rownames(report) <- NULL
    report
}

# Report 3: Suspicious Client Behavior
# One row per account per day with at least one transaction
#
# Returns: data frame sorted ascending by Date, Suspicious first within a date
build_report3 <- function(transactions) {
    #unique (account number, date) pairs = the rows of the report
    account_days <- unique(transactions[, c("account_number", "date")])

    rows <- list()

    for (i in seq_len(nrow(account_days))) {
        acct <- account_days$account_number[i]
        day <- account_days$date[i]

        #transactions made by an account on this day
        todays <- transactions[transactions$account_number == acct & transactions$date == day, ]

        withdrawals <- todays[todays$type == "withdrawal", ]
        deposits <- todays[todays$type == "deposit", ]

        twa <- sum(withdrawals$amount)
        tda <- sum(deposits$amount)
        tov <- if (tda == 0) NA else twa / tda * 100 #NA = no deposits that day (shown as N/A)

        rfl <- if (twa >= 500000 || (!is.na(tov) && tov >= 90)) "Suspicious" else "Normal"

        rows[[i]] <- data.frame(
            AccountNumber          = acct,
            AccountName            = todays$account_name[1],
            Date                   = day,
            TotalWithdrawalAmounts = twa,
            NumberOfDeposits       = nrow(deposits),
            TotalDepositAmounts    = tda,
            TurnoverVelocity       = tov,
            RiskFlagLevel          = rfl
        )
    }

    report <- do.call(rbind, rows)
    report <- report[order(report$Date, report$RiskFlagLevel != "Suspicious", report$AccountNumber), ]
    report$AccountNumber <- NULL
    rownames(report) <- NULL
    report
}

# Report 4: Client Activity Ranking (custom report)
# Ranks clients by the total amount of money moved through their account
#
# Returns: data frame sorted descending by TotalAmountMoved
build_report4 <- function(transactions) {
    accounts <- unique(transactions$account_number)

    rows <- list()

    for (acct in accounts) {
        acct_rows <- transactions[transactions$account_number == acct, ]

        transaction_count <- nrow(acct_rows)
        total_amount_moved <- sum(acct_rows$amount) #deposits + withdrawals

        rows[[acct]] <- data.frame(
            AccountNumber = acct,
            AccountName = acct_rows$account_name[1],
            TransactionCount = transaction_count,
            TotalAmountMoved = total_amount_moved,
            AverageTransactionSize = total_amount_moved / transaction_count
        )
    }

    report <- do.call(rbind, rows)
    report <- report[order(-report$TotalAmountMoved, report$AccountNumber), ]
    report$AccountNumber <- NULL
    rownames(report) <- NULL
    report
}
