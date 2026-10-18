#********************
# Last names: Cauilan, Baun, Dimakuta, Marcelino
# Language: R
# Paradigm(s): Functional, Object-Oriented
#********************

source("definitions.R")

current_balance <- 1000.00
source_currency <- "PHP"

#source: php (last updated: oct 13, 2026)
exchange_rates <- c(
    "Philippine Peso (PHP)" = 1,
    "United States Dollar (USD)" = 0.016,
    "Japanese Yen (JPY)" = 2.52,
    "British Pound Sterling (GBP)" = 0.012,
    "Euro (EUR)" = 0.014,
    "Chinese Yuan Renminni (CNY)" = 0.11
)

#registered accounts
accounts <- load_accounts()

cat("Accounts Returned:\n")
for (account in names(accounts)){
    cat(sprintf("%s\n", account))
}
readline("Please Enter to continue...")

while (TRUE) {
    cat("\014") #clears terminal
    main_menu()
    choice <- readline("choice: ")

    #checks if the user inputs a valid int
    if (!grepl("^-?[0-9]+$", choice) ||
    as.integer(choice) < 1 || as.integer(choice) > 7) {
        cat("Invalid Input!")
        readline("Press Enter to continue...")
        next
    }

    choice <- as.integer(choice)
    #user exits
    if (choice == 7) {
        break
    }

    switch(choice,
        accounts <- register(accounts),
        accounts <- deposit(accounts),
        accounts <- withdraw(accounts),
        currency_exchange(exchange_rates),
        exchange_rates <- record_exchange_rate(exchange_rates),
        show_interest_amt(accounts),
    )
}