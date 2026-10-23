#********************
# Last names: Cauilan, Baun, Dimakuta, Marcelino
# Language: R
# Paradigm(s): Functional, Object-Oriented
#********************

source("definitions.R")

#******************************************************************
#source: php (last updated: oct 13, 2026)
#Exchange rates are stored in a numeric vector resembling a hashmap
#Why numeric vector instead of a list?
#because all values are of the same data type
#******************************************************************
exchange_rates <- c(
    "Philippine Peso (PHP)" = 1,
    "United States Dollar (USD)" = 0.016,
    "Japanese Yen (JPY)" = 2.52,
    "British Pound Sterling (GBP)" = 0.012,
    "Euro (EUR)" = 0.014,
    "Chinese Yuan Renminni (CNY)" = 0.11
)

# registered accounts will be stored here upon runtime
accounts <- load_accounts()

# Main program loop
# Repeats until the user selects option 7 (Exit)

while (TRUE) {
    cat("\014") #clears terminal
    main_menu() #displays main menu
    choice <- readline("choice: ") #gets user input

    #checks if the user inputs a valid int and if it is within range
    if (!grepl("^-?[0-9]+$", choice) ||
    as.integer(choice) < 1 || as.integer(choice) > 7) {
        cat("Invalid Input!\n")
        readline("Press Enter to continue...")
        next
    }

    choice <- as.integer(choice) #parse choice to integer
    #user exits
    if (choice == 7) {
        break
    }

    #switch case
    switch(choice,
        accounts <- register(accounts),
        accounts <- deposit(accounts),
        accounts <- withdraw(accounts),
        currency_exchange(exchange_rates),
        exchange_rates <- record_exchange_rate(exchange_rates),
        show_interest_amt(accounts),
    )
}
