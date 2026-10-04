main_menu <- function(){
    cat(
        "Select Transaction:\n",
        "[1] Register Account Name\n",
        "[2] Deposit Amount\n",
        "[3] Withdraw Amount\n",
        "[4] Currency Exchange\n",
        "[5] Record Exchange Rates\n",
        "[6] Show Interest Amount\n",
        sep = ""
    )

    cat("\n")

    choice <- readline("Choice: ")

    cat("\n")
    cat("***\n")
    cat("choice =", choice, "\n")
}

register <- function(){
    cat("\n")
    cat("Register Account Name\n")
    account_name <- readline("Account Name: ")

    cat("\n")
    cat("***\n")
    cat("Account Name =", account_name, "\n")
}

deposit <- function(current_balance, source_currency){
    cat("\n")
    cat("Deposit Amount\n")
    account_name <- readline("Account Name: ")
    cat(sprintf("Current Balance: %.2f\n", current_balance))
    cat("Currency:", source_currency, "\n")
    cat("\n")

    deposit_amount <- as.numeric(readline("Deposit Amount: ")) #parse to demical
    cat("\n")
    cat("***\n")
    cat("Account Name =", account_name, "\n")
    cat(sprintf("Deposit Amount = %.2f\n", deposit_amount))
}

withdraw <- function(current_balance, source_currency){
    cat("\n")
    cat("Withdraw Amount\n")
    account_name <- readline("Account Name: ")
    cat(sprintf("Current Balance: %.2f\n", current_balance))
    cat("Currency:", source_currency, "\n")
    cat("\n")

    deposit_amount <- as.numeric(readline("Deposit Amount: ")) #parse to demical
    cat("\n")
    cat("***\n")
    cat("Account Name =", account_name, "\n")
    cat(sprintf("Withdraw Amount = %.2f\n", deposit_amount))
}

record_exchange_rate <- function(exchange_rates){
    cat("\n")
    cat("Record Exchange Rate\n")
    cat("\n")
    cat("[1] Philippine Peso (PHP)\n")
    
    i = 2
    for (currency in names(exchange_rates)){
        cat(sprintf("[%d] %s\n", i, currency))
        i <- i + 1
    }

    cat("\n")   
    foreign_currency <- readline("Select Foreign Currency: ")
    exchange_rate <- as.numeric(readline("Exchange Rate: "))
    cat("\n")
    cat("***\n")
    cat("Select Foreign Currency =", foreign_currency, "\n")
    cat(sprintf("Exchange Rate = %.2f\n", exchange_rate))
}

currency_exchange <- function(exchange_rates){
    cat("\n")
    cat("Foreign Currency Exchange\n")
    source_amount <- as.numeric(readline("Source Amount (PHP): "))
    cat("\n")

    cat(
        "Exchanged Currency\n",
        sprintf("[1] Philippine Peso (PHP) = %.2f\n", source_amount),
        sep = ""
    )

    i = 2
    for(currency in names(exchange_rates)){
        rate <- exchange_rates[[currency]] * source_amount
        cat(sprintf("[%d] %s = %.2f\n", i, currency, rate))
        i <- i + 1
    }

    cat("\n")
    cat("***\n")
    cat("Source Currency = Philippines Peso (PHP)\n")
    cat(sprintf("Source Amount (PHP) = %.2f\n", source_amount))
}