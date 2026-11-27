path <- "../bank_transactions.csv"

# Reads one line of user input.
# readline() only waits for input in an interactive session (R console);
# under "Rscript MP_2_R.R" it returns "" immediately, so read stdin instead.
read_input <- function(prompt = "") {
    if (interactive()) return(readline(prompt))
    cat(prompt)
    if (!exists("stdin_con", envir = globalenv())) { #if not open
        assign("stdin_con", file("stdin"), envir = globalenv())
        open(stdin_con)
    }
    line <- readLines(stdin_con, n = 1)
    if (length(line) == 0) quit(save = "no") # input closed (Ctrl+Z / Ctrl+D)
    line
}

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

# Column names of the transactions file, in order
CSV_HEADER <- "account_name,account_number,date,time,currency,amount,type,current_balance"

# Rounds money to 2 decimals, half-up (0.005 -> 0.01), the same way as the
# other language versions. R's round() rounds halves to even and works on the
# binary value, so it can differ by 0.01 on exact ties.
round_money <- function(x) {
    vapply(x, round_money_one, numeric(1), USE.NAMES = FALSE)
}

round_money_one <- function(value) {
    if (is.na(value) || !is.finite(value)) return(value)

    # fast path: the value already has at most 2 decimals
    cents <- value * 100
    if (abs(cents - round(cents)) < 1e-6 && abs(cents) < 1e13) return(round(cents) / 100)

    # shortest decimal text that represents this double exactly
    for (digits in 15:17) {
        text <- format(value, digits = digits, scientific = FALSE)
        if (as.numeric(text) == value) break
    }

    negative <- startsWith(text, "-")
    text <- sub("^-", "", text)
    parts <- strsplit(text, ".", fixed = TRUE)[[1]]
    whole <- parts[1]
    fraction <- if (length(parts) > 1) parts[2] else ""
    fraction <- substr(paste0(fraction, "000"), 1, 3)

    total_cents <- as.numeric(paste0(whole, substr(fraction, 1, 2)))
    if (substr(fraction, 3, 3) >= "5") total_cents <- total_cents + 1

    result <- total_cents / 100
    if (negative) -result else result
}

# Adds numbers one by one in order (like a simple loop in C/Java/Kotlin).
# sum() uses extra precision internally, which can change the last cent
# after rounding, so totals that go into the reports use this instead.
sequential_sum <- function(x) {
    total <- 0
    for (value in x) total <- total + value
    total
}

# Parses a number field; returns NA for anything that is not a plain number
parse_number <- function(text) {
    text <- gsub(",", "", trimws(text), fixed = TRUE)
    is_hex <- grepl("^[+-]?0[xX]", text)
    number <- suppressWarnings(as.numeric(text))
    number[is_hex] <- NA
    number
}

# Reads and validates the transactions file
#
# Parameters:
# - php_only: keep only PHP transactions (FALSE is used to find the
#   highest account number across all currencies)
# - verbose: print the per-row error log and the summary line
#
# Returns: data frame of valid transactions in file order
load_transactions <- function(php_only = TRUE, verbose = TRUE) {
    empty <- data.frame(
        account_name = character(0), account_number = character(0),
        date = as.Date(character(0)), time = character(0),
        currency = character(0), amount = numeric(0), type = character(0),
        current_balance = numeric(0), datetime = as.POSIXct(character(0), tz = "UTC"),
        stringsAsFactors = FALSE
    )

    if (!file.exists(path)) {
        if (verbose) {
            cat("Error: ", path, " not found.\n", sep = "")
            cat("Processing dataset... (0 rows loaded, 0 filtered for PHP currency, 0 error(s) skipped)\n")
        }
        return(empty)
    }

    lines <- readLines(path, warn = FALSE, encoding = "UTF-8")
    lines <- sub("\r$", "", lines)
    lines <- lines[-1]                               # drop the header
    line_numbers <- seq_along(lines) + 1             # row numbers count the header as row 1

    is_blank <- trimws(lines) == ""
    lines <- lines[!is_blank]
    line_numbers <- line_numbers[!is_blank]
    rows_loaded <- length(lines)

    fields <- strsplit(lines, ",", fixed = TRUE)
    field_count <- lengths(fields)
    has_8_columns <- field_count >= 8

    # pad short rows so every column can be read; they are rejected below
    fields <- lapply(fields, function(f) c(f, rep("", max(0, 8 - length(f)))))
    column <- function(i) trimws(vapply(fields, `[`, character(1), i))

    account_name <- column(1)
    account_number <- column(2)
    date_text <- column(3)
    time_text <- column(4)
    currency <- toupper(column(5))
    amount <- parse_number(column(6))
    type <- tolower(column(7))
    current_balance <- parse_number(column(8))

    # dd/mm/yyyy with 2-digit day and month; H:mm:ss with hour 0-23
    date <- as.Date(date_text, format = "%d/%m/%Y")
    date[!grepl("^[0-9]{2}/[0-9]{2}/[0-9]{4}$", date_text)] <- NA
    valid_time <- grepl("^([0-9]|[01][0-9]|2[0-3]):[0-5][0-9]:[0-5][0-9]$", time_text)

    is_valid <- has_8_columns &
                account_name != "" &
                grepl("^[+-]?[0-9]+$", account_number) &
                !is.na(date) &
                valid_time &
                !is.na(amount) & is.finite(amount) & amount > 0 &
                !is.na(current_balance) & is.finite(current_balance) & current_balance >= 0 &
                type %in% c("deposit", "withdrawal")

    if (verbose) {
        for (i in which(!is_valid)) {
            if (!has_8_columns[i]) {
                cat("Row ", line_numbers[i], " skipped: expected 8 columns, found ",
                    field_count[i], "\n", sep = "")
            } else {
                cat("Row ", line_numbers[i], " skipped: invalid or missing field(s) -> ",
                    lines[i], "\n", sep = "")
            }
        }
    }

    transactions <- data.frame(
        account_name = account_name,
        account_number = sub("^\\+?0*([0-9])", "\\1", account_number),
        date = date,
        time = time_text,
        currency = currency,
        amount = round_money(amount),
        type = type,
        current_balance = round_money(current_balance),
        stringsAsFactors = FALSE
    )[is_valid, ]

    errors_skipped <- sum(!is_valid)

    if (php_only) transactions <- transactions[transactions$currency == "PHP", ]

    transactions$datetime <- as.POSIXct(paste(format(transactions$date, "%d/%m/%Y"), transactions$time),
                                        format = "%d/%m/%Y %H:%M:%S", tz = "UTC")
    rownames(transactions) <- NULL

    if (verbose) {
        cat("Processing dataset... (",
            format(rows_loaded, big.mark = ","), " rows loaded, ",
            format(nrow(transactions), big.mark = ","), " filtered for PHP currency, ",
            errors_skipped, " error(s) skipped)\n",
            sep = "")
    }

    transactions
}

# Updates the transactions database
#
# Parameters:
# - transactions: data frame (1 row corresponds to one transaction)
save_transactions <- function(transactions, account_name, account_number,
                        amount, type, new_balance) {
    now <- Sys.time()
    amount <- round_money(amount)
    new_balance <- round_money(new_balance)

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

    #create the file with its header row if it does not exist yet
    if (!file.exists(path)) writeLines(CSV_HEADER, path)

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
        amount = amount,
        type = type,
        current_balance = new_balance,
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
next_account_number <- function() {
    # REQ-0001: next number after the highest in the file, counting every
    # valid row (any currency), not just the PHP rows the program uses
    all_rows <- load_transactions(php_only = FALSE, verbose = FALSE)
    numbers <- as.numeric(all_rows$account_number)

    if (length(numbers) == 0) {
        return("1092000001")
    }

    sprintf("%.0f", max(numbers) + 1)
}

read_amount <- function(prompt, minimum = 0.01) {
    repeat {
        input <- trimws(read_input(prompt))

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
        cat("Register Account\n")
        account_name <- trimws(read_input("Enter Account Name (First Last): "))

        if (!is_valid_name(account_name)) {
            cat("Invalid account name\n")

            choice <- read_input("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        deposit <- read_amount("Initial Deposit: ")

        if (deposit < 5000) {
            cat("Initial Deposit must not be lower than 5000\n")

            choice <- read_input("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }

        acct_num <- next_account_number()

        transactions <- save_transactions(transactions, account_name, acct_num,
                    deposit, "deposit", deposit)

        cat("Account created successfully!\n")
        cat("Account Number: ", acct_num, "\n", sep = "")
        cat("\n")

        choice <- read_input("Back to the Main Menu (Y/N)? ... ")

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
        accno <- read_input("Account Number: ")

        #checks if the account number exists
        if (!account_exists(transactions, accno)) {
            cat("Account not found.\n")

            choice <- read_input("Back to the Main Menu (Y/N)? ... ")

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

        choice <- read_input("Back to the Main Menu (Y/N)? ... ")

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
        accno <- read_input("Account Number: ")

        #checks if the account number exists
        if (!account_exists(transactions, accno)) {
            cat("Account not found.\n")

            choice <- read_input("Back to the Main Menu (Y/N)? ... ")

            if (tolower(choice) == "y") {
                break
            }
            next
        }
  
        current_balance <- get_current_balance(transactions, accno)

        cat(sprintf("Current Balance: %.2f\n", current_balance))
        cat("Currency: PHP\n")
        cat("\n")

        withdraw_amt <- read_amount("Withdraw Amount: ")

        #checks if current balance can handle withdraw amount
        if (withdraw_amt > current_balance) {
            cat("Insufficient Balance!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        account_name <- get_account_name(transactions, accno)
        new_balance <- current_balance - withdraw_amt

        transactions <- save_transactions(transactions, account_name, accno, 
                    withdraw_amt, "withdrawal", new_balance)

        cat(sprintf("Updated Balance: %.2f", new_balance))
        cat("\n")

        choice <- read_input("Back to the Main Menu (Y/N)? ... ")

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

        from <- read_input("FROM: ")
        source_amt <- as.numeric(read_input("Source Amount: "))
        to <- read_input("TO: ")
        
        num_of_currs <- length(exchange_rates)

        if (!grepl("^-?[0-9]+$", from) ||
        !grepl("^-?[0-9]+$", to)
        || as.integer(from) < 1 || as.integer(from) > num_of_currs ||
        as.integer(to) < 1 || as.integer(to) > num_of_currs ||
        is.na(source_amt)) {
            cat("Invalid Input!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
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
        choice <- read_input("Convert another currency (Y/N)? ... ")
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

        foreign_currency <- read_input("Select Foreign Currency: ")
        if (!grepl("^-?[0-9]+$", foreign_currency) || as.integer(foreign_currency) < 1 ||
        as.integer(foreign_currency) > num_of_currs - 1) {
            cat("Invalid Input!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        new_exchange_rate <- as.numeric(read_input("Set New Exchange Rate: "))
        if (is.na(new_exchange_rate)){
            cat("Invalid Input!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        i = as.integer(foreign_currency) + 1 #index of the selected currency
        exchange_rates[i] <- new_exchange_rate

        cat("New exchange rate set for", names(exchange_rates)[i], "\n")

        choice <- read_input("Go Back to the Main Menu (Y/N)? ... ")
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

        accno <- read_input("Account Number: ")

        #checks if an account exists in the registered accounts
        if (!account_exists(transactions, accno)) {
            cat("Account not found!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
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
        days <- read_input("Total Number of Days: ")
        
        #input validation for number of days
        if (!grepl("^-?[0-9]+$", days)) {
            cat("Invalid Input!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }
        days <- as.integer(days)
        
        #input validation for number of days
        if (days < 1) {
            cat("Invalid Input!\n")
            choice <- read_input("Back to the Main Menu (Y/N)? ... ")
            if (tolower(choice) == "y") {
                break
            }
            next
        }

        cat("\n")

        cat(sprintf("%-4s| %-9s| %-10s|\n", "Day", "Interest", "Balance"))
        for (i in 1:days){
            #daily interest is rounded to 2 decimals and added to the balance
            #(the same method as Report 2)
            daily_interest <- round_money(current_bal * (interest_rate / 365))
            current_bal <- round_money(current_bal + daily_interest)
            cat(sprintf("%-4d| %-9.2f| %-10.2f|\n", i, daily_interest, current_bal))
        }

        cat("\n")

        choice <- read_input("Back to the Main Menu (Y/N)? ... ")
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

        choice <- read_input("Enter choice: ")

        #checks if the user inputs a valid int and if it is within range
        if (!is_valid_int_choice(choice, 1, 3)) {
            cat("Invalid Input!\n")
            read_input("Press Enter to continue...")
            next
        }

        choice <- as.integer(choice)

        if (choice == 3) break

        if (choice == 1) {
            transactions <- load_transactions()
            read_input("Press Enter to continue...")
            next
        }

        # choice == 2
        if (nrow(transactions) == 0) {
            cat("No data loaded yet. Please select [1] Load the file first.\n")
            read_input("Press Enter to continue...")
            next
        }

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

        read_input("Press Enter to continue...")
    }

    transactions
}

# Formats money: 1234567.891 -> "1,234,567.89"
format_money <- function(x) {
    formatC(round_money(x), format = "f", digits = 2, big.mark = ",")
}

# Quotes a CSV field only when it contains a comma or a quote
csv_field <- function(text) {
    needs_quotes <- grepl("[,\"]", text)
    text[needs_quotes] <- paste0("\"", gsub("\"", "\"\"", text[needs_quotes], fixed = TRUE), "\"")
    text
}

# Writes lines with "\n" line endings on every operating system
write_lines_lf <- function(lines, filename) {
    con <- file(filename, open = "wb")
    on.exit(close(con))
    writeLines(lines, con, sep = "\n", useBytes = TRUE)
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

    output[] <- lapply(output, as.character)

    # one line per row; a field is quoted only if it contains a comma
    body <- if (nrow(output) == 0) character(0) else
        do.call(paste, c(lapply(output, csv_field), sep = ","))
    write_lines_lf(c(paste(names(output), collapse = ","), body), filename)

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
        account_rows <- transactions[transactions$account_number == accounts[i], ]
        current_balances[i] <- latest_row(account_rows)$current_balance
    }

    is_deposit <- transactions$type == "deposit"
    is_withdrawal <- transactions$type == "withdrawal"

    json <- sprintf(paste0(
        "{\n",
        "  \"total_accounts\": %d,\n",
        "  \"total_deposits\": %.2f,\n",
        "  \"total_withdrawals\": %.2f,\n",
        "  \"total_balance\": %.2f,\n",
        "  \"total_interest_expense\": %.2f\n",
        "}"),
        length(accounts),
        round_money(sequential_sum(transactions$amount[is_deposit])),
        round_money(sequential_sum(transactions$amount[is_withdrawal])),
        round_money(sequential_sum(current_balances)),
        round_money(sequential_sum(report2$ExpectedInterest)))

    write_lines_lf(json, filename)

    cat("Summary State (", filename, "):\n", json, "\n", sep = "")
}

# Returns the account's rows sorted by date and time
# (rows with the same date and time keep their file order)
chronological <- function(rows) {
    rows[order(rows$datetime, method = "radix"), ]
}

# Returns the most recent transaction (by date and time) as a 1-row data frame
latest_row <- function(rows) {
    sorted <- chronological(rows)
    sorted[nrow(sorted), ]
}

# Interest for a number of days at 5% per annum.
# Each day: interest = balance x (0.05 / 365), rounded to 2 decimals and
# added to the balance (the same method as Show Interest Amount).
compute_expected_interest <- function(starting_balance, days = 30) {
    daily_rate <- 0.05 / 365
    balance <- starting_balance
    total_interest <- 0

    for (day in seq_len(days)) {
        interest <- round_money(balance * daily_rate)
        total_interest <- round_money(total_interest + interest)
        balance <- round_money(balance + interest)
    }

    total_interest
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

    accounts <- unique(transactions$account_number) #in order of first appearance

    rows <- list()

    for (acct in accounts) {
        #all transactions made by an account, oldest first
        acct_rows <- chronological(transactions[transactions$account_number == acct, ])

        end_of_day <- numeric(length(days)) #end of day balances

        for (i in seq_along(days)) {
            #latest balance on or before this day (carries over on days with no transactions)
            up_to_day <- acct_rows$current_balance[acct_rows$date <= days[i]]
            end_of_day[i] <- if (length(up_to_day) == 0) 0 else tail(up_to_day, 1)
        }

        rows[[acct]] <- data.frame(
            AccountName = tail(acct_rows$account_name, 1),
            AccountAge = as.integer(reference_date - acct_rows$date[1]),
            CurrentBalance = tail(acct_rows$current_balance, 1),
            AverageDailyBalance = round_money(sequential_sum(end_of_day) / length(days))
        )
    }

    report <- do.call(rbind, rows)
    report <- report[report$AverageDailyBalance >= 100000, ]
    report$PrivilegeTier <- ifelse(report$AverageDailyBalance < 500000, "Silver",
                            ifelse(report$AverageDailyBalance < 1000000, "Gold", "Platinum"))

    #descending ADB; ties keep the order in which accounts first appear in the file
    report <- report[order(-report$AverageDailyBalance, method = "radix"), ]
    rownames(report) <- NULL
    report
}

# Report 2: Interest Expense Projection
# Interest over the next 30 days at 5% per annum, based on the latest balance
#
# Returns: data frame sorted descending by ExpectedInterest
build_report2 <- function(transactions) {
    accounts <- unique(transactions$account_number)

    rows <- list() #each rows of the report will be stored here

    for (acct in accounts) {
        latest <- latest_row(transactions[transactions$account_number == acct, ])
        current_balance <- latest$current_balance

        if (current_balance < 10000) next

        rows[[acct]] <- data.frame(
            AccountName = latest$account_name,
            CurrentBalance = current_balance,
            ExpectedInterest = compute_expected_interest(current_balance, 30)
        )
    }

    report <- do.call(rbind, rows)
    if (is.null(report)) {
        report <- data.frame(AccountName = character(0), CurrentBalance = numeric(0),
                             ExpectedInterest = numeric(0))
    }
    report <- report[order(-report$ExpectedInterest, method = "radix"), ]
    rownames(report) <- NULL
    report
}

# Report 3: Suspicious Client Behavior
# One row per account per day with at least one transaction
#
# Returns: data frame sorted ascending by Date, Suspicious first within a date
build_report3 <- function(transactions) {
    #unique (account number, date) pairs = the rows of the report, in file order
    account_days <- unique(transactions[, c("account_number", "date")])

    #row numbers of each account-day, found once instead of searching
    #the whole table on every pass of the loop
    keys <- paste(transactions$account_number, transactions$date)
    rows_by_key <- split(seq_len(nrow(transactions)), keys)

    n <- nrow(account_days)

    #columns as plain vectors: indexing a vector is much faster than
    #subsetting a data frame inside a loop
    amounts <- transactions$amount
    types <- transactions$type
    datetimes <- transactions$datetime
    account_names <- transactions$account_name

    #one vector per column, filled in the loop and combined at the end
    #(building 8,000 one-row data frames and binding them is very slow in R)
    names_col <- character(n)
    withdrawals_col <- numeric(n)
    deposit_count_col <- integer(n)
    deposits_col <- numeric(n)
    velocity_col <- numeric(n)
    flag_col <- character(n)

    for (i in seq_len(n)) {
        day <- account_days$date[i]
        key <- paste(account_days$account_number[i], day)

        #row numbers of this account's transactions on this day
        idx <- rows_by_key[[key]]
        is_withdrawal <- types[idx] == "withdrawal"
        is_deposit <- types[idx] == "deposit"

        twa <- round_money(sequential_sum(amounts[idx][is_withdrawal]))
        tda <- round_money(sequential_sum(amounts[idx][is_deposit]))

        #no deposits that day: velocity is shown as 0.00
        raw_velocity <- if (tda > 0) twa / tda * 100 else 0

        #name on the most recent transaction of the day
        latest <- idx[order(datetimes[idx], method = "radix")][length(idx)]

        names_col[i] <- account_names[latest]
        withdrawals_col[i] <- twa
        deposit_count_col[i] <- sum(is_deposit)
        deposits_col[i] <- tda
        velocity_col[i] <- round_money(raw_velocity)
        flag_col[i] <- if (twa >= 500000 || raw_velocity >= 90) "Suspicious" else "Normal"
    }

    report <- data.frame(
        AccountName            = names_col,
        Date                   = account_days$date,
        TotalWithdrawalAmounts = withdrawals_col,
        NumberOfDeposits       = deposit_count_col,
        TotalDepositAmounts    = deposits_col,
        TurnoverVelocity       = velocity_col,
        RiskFlagLevel          = flag_col
    )

    #by date, Suspicious first; ties keep the order of first appearance in the file
    report <- report[order(report$Date, report$RiskFlagLevel != "Suspicious", method = "radix"), ]
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
        total_amount_moved <- round_money(sequential_sum(acct_rows$amount)) #deposits + withdrawals

        rows[[acct]] <- data.frame(
            AccountName = latest_row(acct_rows)$account_name,
            TransactionCount = transaction_count,
            TotalAmountMoved = total_amount_moved,
            AverageTransactionSize = round_money(total_amount_moved / transaction_count)
        )
    }

    report <- do.call(rbind, rows)
    report <- report[order(-report$TotalAmountMoved, method = "radix"), ]
    rownames(report) <- NULL
    report
}
