#********************
# Last names: Cauilan, Baun, Dimakuta, Marcelino
# Language: R
# Paradigm(s): Functional, Object-Oriented
#********************

source("definitions.R")

current_balance <- 1000.00
source_currency <- "PHP"

exchange_rates <- list(
    "United States Dollar (USD)" = 62.00,
    "Japanese Yen (JPY)" = 0.40,
    "British Pound Sterling (GBP)" = 84.00,
    "Euro (EUR)" = 72.00,
    "Chinese Yuan Renminni (CNY)" = 9.00
)

main_menu()
register()
deposit(current_balance, source_currency)
withdraw(current_balance, source_currency)
record_exchange_rate(exchange_rates)
currency_exchange(exchange_rates)