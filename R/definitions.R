# Displays the main menu
main_menu <- function() {
    cat(
        "Select Transaction:\n",
        "[1] Register Account Name\n",
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
load_transactions <- function(path) {
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
                        amount, type, new_balance, path = "../bank_transactions.csv") {
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

# Deposits an amount to an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
deposit <- function(transactions){
    while(TRUE){
        cat("\014")
        cat("Deposit Amount\n")
        account_name <- readline("Account Number: ")

        #checks if account number exists
        if(!(account_name %in% names(accounts))) {
            cat("Account not found!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        current_balance <- accounts[[account_name]]$balance

        cat(sprintf("Current Balance: %.2f\n", current_balance))
        cat("Currency: PHP\n")
        cat("\n")

        deposit_amt <- as.numeric(readline("Deposit Amount: "))

        if(is.na(deposit_amt) || deposit_amt < 0){
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        #update balance
        accounts[[account_name]]$balance <- current_balance + deposit_amt
        
        cat(sprintf("Updated Balance: %.2f\n", accounts[[account_name]]$balance))
        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y"){
            break
        }
    }

    update_accounts(accounts)
    return(accounts)
}

# Withdraws an amount from an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
withdraw <- function(accounts){
    while(TRUE){
        cat("\014")
        cat("Withdraw Amount\n")
        account_name <- readline("Account Name: ")

        #checks if account name exists
        if(!(account_name %in% names(accounts))){
            cat("Account not found!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        current_balance <- accounts[[account_name]]$balance

        cat(sprintf("Current Balance: %.2f\n", current_balance))
        cat("Currency: PHP\n")
        cat("\n")

        withdraw_amt <- as.numeric(readline("Withdraw Amount: "))

        #input validation
        if(is.na(withdraw_amt) || withdraw_amt < 0){
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        #checks if current balance can handle withdraw amount
        if(withdraw_amt > current_balance){
            cat("Insufficient Balance!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        #update balance
        accounts[[account_name]]$balance <- current_balance - withdraw_amt

        cat(sprintf("Updated Balance: %.2f", accounts[[account_name]]$balance))
        cat("\n")

        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y"){
            break
        }
    }

    update_accounts(accounts)
    return(accounts)
}

# Allows a user to view the equivalent amount of one currency to another
#
# Parameters:
# - exchange_rates: Numeric vector containing exchange rates
#   relative to PHP
currency_exchange <- function(exchange_rates){
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
# - accounts: List of registered accounts
show_interest_amt <- function(accounts){
    while(TRUE){
        cat("\014")
        cat("Show Interest Amount\n")

        account_name <- readline("Account Name: ")

        #checks if an account exists in the registered accounts
        if (!(account_name %in% names(accounts))) {
            cat("Account not found!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        interest_rate <- 0.05

        current_bal <- accounts[[account_name]]$balance
        cat(sprintf("Current Balance: %.2f\n", current_bal))
        cat("Currency: PHP\n")
        cat("Interest Rate: 5%\n")
        cat("\n")
        days <- readline("Total Number of Days: ")
        
        #input validation for number of days
        if(!grepl("^-?[0-9]+$", days)) {
            cat("Invalid Input!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }
        days <- as.integer(days)
        
        #input validation for number of days
        if(days < 1){
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

