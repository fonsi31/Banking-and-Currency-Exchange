/********************
Last names: Baun, Cauilan, Dimakuta, Marcelino
Language: Kotlin
Paradigm(s): Multi-Paradigm
 ********************/

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.system.exitProcess
import java.io.File
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

// ---------------------------------------------------------------------------
// Constants and data
// ---------------------------------------------------------------------------

const val ANNUAL_INTEREST_RATE = 0.05   // 5% per annum
const val DAYS_PER_YEAR = 365
const val BASE_CURRENCY_INDEX = 0       // PHP is the base currency
const val STARTING_BALANCE = 1000.00
const val CSV_FILE_PATH = "bank_transactions.csv"
const val MIN_INITIAL_DEPOSIT = 5000.00
const val DEFAULT_START_ACCOUNT_NUMBER = 1092000001L

val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
val TIME_PARSE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm:ss")

/** [rate] is how many PHP one unit of this currency is worth. */
data class Currency(val name: String, val code: String, var rate: Double)

val currencies = listOf(
    Currency("Philippine Peso (PHP)", "PHP", 1.00),
    Currency("United States Dollar (USD)", "USD", 62.00),
    Currency("Japanese Yen (JPY)", "JPY", 0.40),
    Currency("British Pound Sterling (GBP)", "GBP", 84.00),
    Currency("Euro (EUR)", "EUR", 72.00),
    Currency("Chinese Yuan Renminbi (CNY)", "CNY", 9.00),
)

val baseCurrency = currencies[BASE_CURRENCY_INDEX]


data class Account
    (
            val accountNumber: Long,
            val name: String,
            var balance: Double,
            val currency: Currency = baseCurrency
    )

data class BankTransaction(
    val accountName: String,
    val accountNumber: Long,
    val date: LocalDate,
    val time: LocalTime,
    val currency: String,
    val amount: Double,
    val type: String,
    val currentBalance: Double
)

data class LoadResult(
    val transactions: List<BankTransaction>,
    val totalRows: Int,
    val skippedErrors: Int,
    val skippedNonPhp: Int
)

class AccountStore(private val csvFile: File = File(CSV_FILE_PATH)) {
    val accounts: MutableMap<Long, Account> = mutableMapOf()
    private var highestAccountNumber: Long = DEFAULT_START_ACCOUNT_NUMBER - 1

    init {
        loadAccountsFromCsv()
    }

    private fun loadAccountsFromCsv() {
        if (!csvFile.exists()) return

        csvFile.useLines { lines ->
            lines.drop(1).forEach { line ->
                val parts = line.split(",")
                if (parts.size < 8) return@forEach

                val name = parts[0].trim()
                val accNum = parts[1].trim().toLongOrNull() ?: return@forEach
                val balance = parts[7].trim().toDoubleOrNull() ?: return@forEach

                if (accNum > highestAccountNumber) {
                    highestAccountNumber = accNum
                }
                accounts[accNum] = Account(accNum, name, balance)
            }

        }

    }
    fun nextAccountNumber(): Long {
        highestAccountNumber += 1
        return highestAccountNumber
    }

    fun appendTransaction(
        account: Account,
        type: String,
        amount: Double
    ) {
        val date = LocalDate.now().format(DATE_FORMATTER)
        val time = LocalTime.now().format(TIME_FORMATTER)
        val row = "${account.name},${account.accountNumber},$date,$time,${account.currency.code},${amount.money()},$type,${account.balance.money()}"


        if (!csvFile.exists()) {
            csvFile.writeText("account_name,account_number,date,time,currency,amount,type,current_balance\n")
        }

        csvFile.appendText("$row\n")
    }

    fun findAccount(query: String): Account? {

        query.toLongOrNull()?.let { accNum ->
            return accounts[accNum]
        }

        return accounts.values.firstOrNull {
            it.name.equals(query, ignoreCase = true)
        }
    }

    fun promptAccount(): Account? {
        if (accounts.isEmpty()) {
            println("No accounts registered yet. Select [1] Register Account first.")
            return null
        }

        val query = promptNonBlank("Account Name or Number: ")
        val account = findAccount(query)
        if (account == null) {
            println("Account not found.")
        }
        return account
    }

}


fun loadTransactions(csvFile: File = File(CSV_FILE_PATH)): LoadResult {
    if (!csvFile.exists()) {
        println("Error: ${csvFile.path} not found.")
        return LoadResult(emptyList(), 0, 0, 0)
    }

    val cleaned = mutableListOf<BankTransaction>()
    var totalRows = 0
    var skippedErrors = 0
    var skippedNonPhp = 0

    csvFile.useLines { lines ->
        lines.drop(1).forEachIndexed { index, rawLine ->
            if (rawLine.isBlank()) return@forEachIndexed
            totalRows++
            val lineNumber = index + 2 // +1 for 0-based index, +1 for header row

            val parts = rawLine.split(",")
            if (parts.size < 8) {
                println("Row $lineNumber skipped: expected 8 columns, found ${parts.size}")
                skippedErrors++
                return@forEachIndexed
            }

            val name = parts[0].trim()
            val accNum = parts[1].trim().toLongOrNull()
            val date = runCatching { LocalDate.parse(parts[2].trim(), DATE_FORMATTER) }.getOrNull()
            val time = runCatching { LocalTime.parse(parts[3].trim(), TIME_PARSE_FORMATTER) }.getOrNull()
            val currency = parts[4].trim().uppercase()
            val amount = parts[5].trim().replace(",", "").toDoubleOrNull()
            val type = parts[6].trim().lowercase()
            val balance = parts[7].trim().replace(",", "").toDoubleOrNull()

            // Basic validation (REQ-0004 & REQ-0005)
            val isValidType = type == "deposit" || type == "withdrawal"
            if (name.isEmpty() || accNum == null || date == null || time == null ||
                amount == null || !amount.isFinite() || balance == null || !balance.isFinite() ||
                !isValidType
            ) {
                println("Row $lineNumber skipped: invalid or missing field(s) -> $rawLine")
                skippedErrors++
                return@forEachIndexed
            }

            // Filter PHP currency only (REQ-0003)
            if (currency != "PHP") {
                skippedNonPhp++
                return@forEachIndexed
            }

            cleaned.add(
                BankTransaction(
                    accountName = name,
                    accountNumber = accNum,
                    date = date,
                    time = time,
                    currency = currency,
                    amount = amount.roundMoney(),
                    type = type,
                    currentBalance = balance.roundMoney()
                )
            )
        }
    }

    return LoadResult(cleaned, totalRows, skippedErrors, skippedNonPhp)
}
// ---------------------------------------------------------------------------
// Helpers: formatting, math, input
// ---------------------------------------------------------------------------

fun Double.money(): String = "%.2f".format(Locale.US, this)

fun Double.roundMoney(): Double =
    BigDecimal.valueOf(this).setScale(2, RoundingMode.HALF_UP).toDouble()

fun convert(amount: Double, from: Currency, to: Currency): Double =
    amount * from.rate / to.rate

fun printCurrencies() =
    currencies.forEachIndexed { i, c -> println("[${i + 1}] ${c.name}") }

fun printSeparator() {
    println("=".repeat(38))
    println()
}

fun prompt(label: String): String {
    print(label)
    return readlnOrNull()?.trim() ?: exitProcess(0)
}

fun promptNonBlank(label: String): String {
    while (true) {
        val text = prompt(label)
        if (text.isNotEmpty()) return text
        println("Input cannot be empty.")
    }
}


fun promptSelection(label: String, range: IntRange): Int {
    while (true) {
        val value = prompt(label).removeSurrounding("[", "]").toIntOrNull()
        if (value != null && value in range) return value
        println("Invalid option. Enter a number from ${range.first} to ${range.last}.")
    }
}

fun promptCurrency(label: String): Currency =
    currencies[promptSelection(label, 1..currencies.size) - 1]

fun promptAmount(label: String): Double {
    while (true) {
        val value = prompt(label).replace(",", "").toDoubleOrNull()
        if (value != null && value.isFinite() && value > 0) return value
        println("Invalid amount. Enter a number greater than 0.")
    }
}

fun promptDays(label: String): Int {
    while (true) {
        val value = prompt(label).removeSuffix("days").removeSuffix("day").trim().toIntOrNull()
        if (value != null && value > 0) return value
        println("Invalid number of days. Enter a whole number greater than 0.")
    }
}

fun promptYesNo(label: String): Boolean {
    while (true) {
        when (prompt(label).lowercase()) {
            "y" -> return true
            "n" -> return false
            else -> println("Please enter Y or N.")
        }
    }
}

fun Account.requireRegistered(): Boolean {
    if (name == null) {
        println("No account registered yet. Select [1] Register Account Name first.")
        return false
    }
    return true
}

fun promptAmountAtLeast(label: String, min: Double): Double {
    while (true) {
        val value = promptAmount(label)
        if (value >= min) return value
        println("Amount must be at least ${min.money()}.")
    }
}

// ---------------------------------------------------------------------------
// Transactions (one class per menu option)
// ---------------------------------------------------------------------------

interface Transaction {
    /** If true, the app goes straight back to the menu instead of asking "Back to the Main Menu (Y/N)". */
    val returnsToMenuAutomatically: Boolean get() = false

    fun execute()
}


class RegisterAccount(private val store: AccountStore) : Transaction {
    override val returnsToMenuAutomatically = true

    override fun execute() {
        do {
            println("Register Account")
            val name = promptNonBlank("Account Name: ")
            val initialDeposit = promptAmountAtLeast(
                "Initial Deposit (min ${MIN_INITIAL_DEPOSIT.money()}): ",
                MIN_INITIAL_DEPOSIT
            ).roundMoney()

            val accNum = store.nextAccountNumber()
            val newAccount = Account(accNum, name, initialDeposit)
            store.accounts[accNum] = newAccount
            store.appendTransaction(newAccount, "deposit", initialDeposit)

            println("Account created successfully!")
            println("Account Number: $accNum")
            println()
        } while (promptYesNo("Register another account? (Y/N): "))
    }
}


class Deposit(private val store: AccountStore) : Transaction {
    override val returnsToMenuAutomatically = true

    override fun execute() {
        do {
            println("Deposit Amount")
            val account = store.promptAccount() ?: return

            println("Account Name: ${account.name}")
            println("Account Number: ${account.accountNumber}")
            println("Current Balance: ${account.balance.money()}")
            println("Currency: ${account.currency.code}")
            println()

            println("Deposit Currency Options:")
            printCurrencies()
            println()
            val depositCurrency = promptCurrency("Deposit Currency: ")
            val amount = promptAmount("Deposit Amount: ")

            val credited = convert(amount, depositCurrency, account.currency).roundMoney()
            if (depositCurrency != account.currency) {
                println("Converted Amount (${account.currency.code}): ${credited.money()}")
            }

            account.balance = (account.balance + credited).roundMoney()
            store.appendTransaction(account, "deposit", credited)

            println("Updated Balance: ${account.balance.money()}")
            println()
        } while (promptYesNo("Deposit another amount? (Y/N): "))
    }
}


class Withdraw(private val store: AccountStore) : Transaction {
    override val returnsToMenuAutomatically = true

    override fun execute() {
        do {
            println("Withdraw Amount")
            val account = store.promptAccount() ?: return

            println("Account Name: ${account.name}")
            println("Account Number: ${account.accountNumber}")
            println("Current Balance: ${account.balance.money()}")
            println("Currency: ${account.currency.code}")
            println()

            println("Withdraw Currency Options:")
            printCurrencies()
            println()
            val withdrawCurrency = promptCurrency("Withdraw Currency: ")
            val amount = promptAmount("Withdraw Amount: ")

            val debited = convert(amount, withdrawCurrency, account.currency).roundMoney()
            if (withdrawCurrency != account.currency) {
                println("Converted Amount (${account.currency.code}): ${debited.money()}")
            }

            if (debited > account.balance) {
                println("Insufficient balance. Withdrawal cancelled.")
                println("Updated Balance: ${account.balance.money()}")
            } else {
                account.balance = (account.balance - debited).roundMoney()
                store.appendTransaction(account, "withdrawal", debited)
                println("Updated Balance: ${account.balance.money()}")
            }
            println()
        } while (promptYesNo("Withdraw another amount? (Y/N): "))
    }
}


class RecordExchangeRate : Transaction {
    override fun execute() {
        println("Record Exchange Rate")
        println()
        printCurrencies()
        println()

        val currency = promptCurrency("Select Foreign Currency: ")
        if (currency == baseCurrency) {
            println("Exchange Rate: ${baseCurrency.rate.money()}")
            println("${baseCurrency.code} is the base currency, so its rate stays at 1.00.")
            return
        }

        println("Current Exchange Rate: ${currency.rate.money()}")
        currency.rate = promptAmount("Exchange Rate (PHP per 1 ${currency.code}): ")
        println("Exchange Rate: ${currency.rate.money()}")
    }
}


class CurrencyExchange : Transaction {
    override val returnsToMenuAutomatically = true

    override fun execute() {
        do {
            println("Foreign Currency Exchange")
            println("Source Currency Option:")
            printCurrencies()
            println()

            val source = promptCurrency("Source Currency: ")
            val amount = promptAmount("Source Amount: ")
            println()

            println("Exchanged Currency Options:")
            printCurrencies()
            println()

            val target = promptCurrency("Exchange Currency: ")
            println("Exchange Amount: ${convert(amount, source, target).money()}")
            println()
        } while (promptYesNo("Convert another currency (Y/N)? "))
    }
}


class ShowInterestAmount(private val store: AccountStore) : Transaction {
    override fun execute() {
        println("Show Interest Amount")
        val account = store.promptAccount() ?: return

        println("Account Name: ${account.name}")
        println("Current Balance: ${account.balance.money()}")
        println("Currency: ${account.currency.code}")
        println("Interest Rate: ${(ANNUAL_INTEREST_RATE * 100).toInt()}%")
        println()

        val days = promptDays("Total Number of Days: ")
        println()

        println("Day | Interest | Balance |")
        var balance = account.balance
        for (day in 1..days) {
            // Daily Interest = End-of-Day Balance x (Annual Interest Rate / 365)
            val dailyRate = ANNUAL_INTEREST_RATE / DAYS_PER_YEAR
            val interest = (balance * dailyRate).roundMoney()
            balance = (balance + interest).roundMoney()
            println("%-3d | %-8.2f | %.2f |".format(Locale.US, day, interest, balance))
        }
    }
}


class GenerateReports : Transaction {
    override val returnsToMenuAutomatically = true

    private var loadedTransactions: List<BankTransaction>? = null

    override fun execute() {
        while (true) {
            println("Generate Reports")
            println("[1] Load the file")
            println("[2] Generate Reports")
            println("[3] Back to Main Menu")
            println()

            when (promptSelection("Enter choice: ", 1..3)) {
                1 -> {
                    val result = loadTransactions()
                    loadedTransactions = result.transactions
                    println(
                        "Processing dataset... (${"%,d".format(Locale.US, result.totalRows)} rows loaded, " +
                                "${"%,d".format(Locale.US, result.transactions.size)} filtered for PHP currency, " +
                                "${result.skippedErrors} error(s) skipped)"
                    )
                    println()
                }
                2 -> {
                    val data = loadedTransactions
                    if (data.isNullOrEmpty()) {
                        println("No data loaded yet. Please select [1] Load the file first.")
                        println()
                        continue
                    }
                    println("Generating reports...")
                    // We will call Reports 1–4 and summary.json here in Steps 4–7!
                    println("Ready to generate reports from ${data.size} PHP transactions.")
                    println()
                }
                3 -> return
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Main menu
// ---------------------------------------------------------------------------

fun printMenu() {
    println("Select Transaction:")
    println("[1] Register Account")
    println("[2] Deposit Amount")
    println("[3] Withdraw Amount")
    println("[4] Currency Exchange")
    println("[5] Record Exchange Rates")
    println("[6] Show Interest Amount")
    println("[7] Generate Reports")
}

fun main() {
    val store = AccountStore()
    val transactions: Map<Int, Transaction> = mapOf(
        1 to RegisterAccount(store),
        2 to Deposit(store),
        3 to Withdraw(store),
        4 to CurrencyExchange(),
        5 to RecordExchangeRate(),
        6 to ShowInterestAmount(store),
        7 to GenerateReports(),
    )


    do {
        printMenu()
        println()
        val choice = promptSelection("Choice: ", 1..transactions.size)
        println()

        printSeparator()
        val transaction = transactions.getValue(choice)
        transaction.execute()
        println()
    } while (transaction.returnsToMenuAutomatically || promptYesNo("Back to the Main Menu (Y/N): "))

    println()
    println("Thank you for banking with us.")
}
