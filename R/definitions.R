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
        "[7] Exit\n",
        sep = ""
    )
}

# reads and loads the account records from the database
#
# Returns: The registered accounts stored in a nested list
load_accounts <- function() {
    tryCatch({
        accounts <- jsonlite::fromJSON("registered_accounts.json",
        simplifyVector = FALSE)
   
        if (!is.list(accounts)){
            return(list())
        }

        for (name in names(accounts)){
            account <- accounts[[name]]

            if (!is.list(account) || is.null(account) || !is.numeric(account$balance)) {
                return(list())
            }
        }

        return(accounts)
    }, error = function(e) {
        return(list())
    })
}

# Updates the accounts database
#
# Parameters:
# - accounts: the list of registered accounts

update_accounts <- function(accounts) {
    tryCatch({
        jsonlite::write_json(accounts, "registered_accounts.json",
        auto_unbox=TRUE, pretty=TRUE)
        TRUE
    }, error = function(e) {
        cat("Failed to save account: ", conditionMessage(e), "\n")
        FALSE
    })
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

# Registers an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
register <- function(accounts) {
    while(TRUE){
        cat("\014")
        cat("Register Account Name\n")
        account_name <- readline("Enter Account Name: ")

        #checks if account name already exists in the registered accounts
        if(account_name %in% names(accounts)){
            cat("Account already exists!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        accounts[[account_name]]$balance <- 0.0
        update_accounts(accounts)

        cat("Account Created Successfuly!\n")
        cat("Hi", account_name, "!\n")
        cat("\n")
        choice <- readline("Back to the Main Menu (Y/N)? ... ")

        if (tolower(choice) == "y"){
            break
        }
    }

    update_accounts(accounts)
    return(accounts)
}

# Deposits an amount to an account
#
# Parameters:
# - accounts: list of registered accounts
#
# Returns: The new updated list of accounts
deposit <- function(accounts){
    while(TRUE){
        cat("\014")
        cat("Deposit Amount\n")
        account_name <- readline("Account Name: ")

        #checks if account name exists
        if(!(account_name %in% names(accounts))) {
            cat("Account not found!\n")
            choice <- readline("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y"){
                break
            }
            next
        }

        current_balance <- accounts[[account_name]]$balance

        cat("Current Balance:", current_balance, "\n")
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
        
        cat("Updated Balance:", accounts[[account_name]]$balance, "\n")
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