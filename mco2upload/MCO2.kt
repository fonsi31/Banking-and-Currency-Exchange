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
import java.time.YearMonth
import java.time.temporal.ChronoUnit
// ---------------------------------------------------------------------------
// Constants and data
// ---------------------------------------------------------------------------

const val ANNUAL_INTEREST_RATE = 0.05
const val DAYS_PER_YEAR = 365
const val BASE_CURRENCY_INDEX = 0
const val CSV_FILE_PATH = "bank_transactions.csv"
const val MIN_INITIAL_DEPOSIT = 5000.00
const val DEFAULT_START_ACCOUNT_NUMBER = 1092000001L
const val PROJECTION_DAYS = 30
const val MIN_INTEREST_BALANCE = 10_000.00
const val SUSPICIOUS_WITHDRAWAL_THRESHOLD = 500_000.00
const val SUSPICIOUS_VELOCITY_THRESHOLD = 90.0

val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(java.time.format.ResolverStyle.STRICT)
val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
val TIME_PARSE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm:ss").withResolverStyle(java.time.format.ResolverStyle.STRICT)

/** Orders by date then time; sortedWith is stable so ties keep file order. */
val TXN_ORDER: Comparator<BankTransaction> = compareBy<BankTransaction> { it.date }.thenBy { it.time }

fun List<BankTransaction>.latest(): BankTransaction = this.sortedWith(TXN_ORDER).last()

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


data class Account(
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
data class PrivilegeTierRow(
    val accountName: String,
    val accountAge: Long,
    val currentBalance: Double,
    val averageDailyBalance: Double,
    val privilegeTier: String
)

data class InterestProjectionRow(
    val accountName: String,
    val currentBalance: Double,
    val expectedInterest: Double
)
data class SuspiciousBehaviorRow(
    val accountName: String,
    val date: LocalDate,
    val totalWithdrawalAmounts: Double,
    val numberOfDeposits: Int,
    val totalDepositAmounts: Double,
    val turnoverVelocity: Double,
    val riskFlagLevel: String
)

data class MonthlyCashFlowRow(
    val month: YearMonth,
    val transactionCount: Int,
    val totalDeposits: Double,
    val totalWithdrawals: Double,
    val netCashFlow: Double
)

class AccountStore(private val csvFile: File = File(CSV_FILE_PATH)) {
    val accounts: MutableMap<Long, Account> = mutableMapOf()
    private var highestAccountNumber: Long = DEFAULT_START_ACCOUNT_NUMBER - 1

    init {
        loadAccountsFromCsv()
    }

    private fun loadAccountsFromCsv() {
        if (!csvFile.exists()) return

        // Highest account number is taken from every valid row (any currency).
        val all = loadTransactions(csvFile, verbose = false, phpOnly = false).transactions
        for (t in all) if (t.accountNumber > highestAccountNumber) highestAccountNumber = t.accountNumber

        // Current balance = balance of the chronologically latest PHP transaction.
        all.filter { it.currency == "PHP" }
            .groupBy { it.accountNumber }
            .forEach { (accNum, txns) ->
                val last = txns.latest()
                accounts[accNum] = Account(accNum, last.accountName, last.currentBalance)
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


fun loadTransactions(
    csvFile: File = File(CSV_FILE_PATH),
    verbose: Boolean = true,
    phpOnly: Boolean = true
): LoadResult {
    if (!csvFile.exists()) {
        if (verbose) println("Error: ${csvFile.path} not found.")
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
                if (verbose) println("Row $lineNumber skipped: expected 8 columns, found ${parts.size}")
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


            val isValidType = type == "deposit" || type == "withdrawal"
            if (name.isEmpty() || accNum == null || date == null || time == null ||
                amount == null || !amount.isFinite() || amount <= 0.0 ||
                balance == null || !balance.isFinite() || balance < 0.0 ||
                !isValidType
            ) {
                if (verbose) println("Row $lineNumber skipped: invalid or missing field(s) -> $rawLine")
                skippedErrors++
                return@forEachIndexed
            }


            if (phpOnly && currency != "PHP") {
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


fun generatePrivilegeTierReport(
    transactions: List<BankTransaction>,
    outputFile: File = File("report1_banking_privilege_tiers.csv")
): List<PrivilegeTierRow> {
    if (transactions.isEmpty()) return emptyList()


    val latestDate = transactions.maxOf { it.date }
    val lastMonth = YearMonth.from(latestDate).minusMonths(1)
    val monthStart = lastMonth.atDay(1)
    val daysInMonth = lastMonth.lengthOfMonth()


    val byAccount = transactions.groupBy { it.accountNumber }

    val rows = mutableListOf<PrivilegeTierRow>()

    for ((_, accountTxns) in byAccount) {

        val sorted = accountTxns.sortedWith(TXN_ORDER)

        val name = sorted.last().accountName
        val openedDate = sorted.first().date
        val accountAge = ChronoUnit.DAYS.between(openedDate, latestDate)
        val currentBalance = sorted.last().currentBalance


        var runningBalance = sorted.lastOrNull { it.date.isBefore(monthStart) }?.currentBalance ?: 0.0

        val eodByDate = mutableMapOf<LocalDate, Double>()
        for (txn in sorted) {
            if (YearMonth.from(txn.date) == lastMonth) {
                eodByDate[txn.date] = txn.currentBalance
            }
        }


        var sumEod = 0.0
        for (dayNum in 1..daysInMonth) {
            val day = lastMonth.atDay(dayNum)
            if (eodByDate.containsKey(day)) {
                runningBalance = eodByDate.getValue(day)
            }
            sumEod += runningBalance
        }

        val adb = (sumEod / daysInMonth).roundMoney()


        if (adb >= 100_000.00) {
            val tier = when {
                adb >= 1_000_000.00 -> "Platinum"
                adb >= 500_000.00 -> "Gold"
                else -> "Silver"
            }
            rows.add(PrivilegeTierRow(name, accountAge, currentBalance, adb, tier))
        }
    }


    val sortedRows = rows.sortedByDescending { it.averageDailyBalance }


    val csvContent = buildString {
        appendLine("AccountName,AccountAge,CurrentBalance,AverageDailyBalance,PrivilegeTier")
        for (r in sortedRows) {
            appendLine(
                listOf(
                    csvField(r.accountName),
                    r.accountAge.toString(),
                    csvField(r.currentBalance.moneyComma()),
                    csvField(r.averageDailyBalance.moneyComma()),
                    r.privilegeTier
                ).joinToString(",")
            )
        }
    }
    outputFile.writeText(csvContent)

    return sortedRows
}

fun generateInterestProjectionReport(
    transactions: List<BankTransaction>,
    outputFile: File = File("report2_interest_expense_projection.csv")
): List<InterestProjectionRow> {
    if (transactions.isEmpty()) return emptyList()


    val byAccount = transactions.groupBy { it.accountNumber }
    val rows = mutableListOf<InterestProjectionRow>()

    for ((_, accountTxns) in byAccount) {

        val latestTxn = accountTxns.latest()

        val name = latestTxn.accountName
        val currentBalance = latestTxn.currentBalance


        if (currentBalance >= MIN_INTEREST_BALANCE) {
            val expectedInterest = computeExpectedInterest(currentBalance, PROJECTION_DAYS)
            rows.add(InterestProjectionRow(name, currentBalance, expectedInterest))
        }
    }


    val sortedRows = rows.sortedByDescending { it.expectedInterest }


    val csvContent = buildString {
        appendLine("AccountName,CurrentBalance,ExpectedInterest")
        for (r in sortedRows) {
            appendLine(
                listOf(
                    csvField(r.accountName),
                    csvField(r.currentBalance.moneyComma()),
                    csvField(r.expectedInterest.moneyComma())
                ).joinToString(",")
            )
        }
    }
    outputFile.writeText(csvContent)

    return sortedRows
}

fun generateSuspiciousBehaviorReport(
    transactions: List<BankTransaction>,
    outputFile: File = File("report3_suspicious_client_behavior.csv")
): List<SuspiciousBehaviorRow> {
    if (transactions.isEmpty()) return emptyList()


    val byAccountAndDate = transactions.groupBy { it.accountNumber to it.date }
    val rows = mutableListOf<SuspiciousBehaviorRow>()

    for ((key, dayTxns) in byAccountAndDate) {
        val date = key.second
        val name = dayTxns.latest().accountName


        val withdrawals = dayTxns.filter { it.type == "withdrawal" }
        val deposits = dayTxns.filter { it.type == "deposit" }

        val totalWithdrawals = withdrawals.sumOf { it.amount }.roundMoney()
        val numberOfDeposits = deposits.size
        val totalDeposits = deposits.sumOf { it.amount }.roundMoney()


        val rawVelocity = if (totalDeposits > 0.0) (totalWithdrawals / totalDeposits) * 100.0 else 0.0
        val turnoverVelocity = rawVelocity.roundMoney()


        val riskFlag = if (
            totalWithdrawals >= SUSPICIOUS_WITHDRAWAL_THRESHOLD ||
            rawVelocity >= SUSPICIOUS_VELOCITY_THRESHOLD
        ) {
            "Suspicious"
        } else {
            "Normal"
        }

        rows.add(
            SuspiciousBehaviorRow(
                accountName = name,
                date = date,
                totalWithdrawalAmounts = totalWithdrawals,
                numberOfDeposits = numberOfDeposits,
                totalDepositAmounts = totalDeposits,
                turnoverVelocity = turnoverVelocity,
                riskFlagLevel = riskFlag
            )
        )
    }


    val sortedRows = rows.sortedWith(
        compareBy<SuspiciousBehaviorRow> { it.date }
            .thenByDescending { if (it.riskFlagLevel == "Suspicious") 1 else 0 }
    )

    val csvContent = buildString {
        appendLine("AccountName,Date,TotalWithdrawalAmounts,NumberOfDeposits,TotalDepositAmounts,TurnoverVelocity,RiskFlagLevel")
        for (r in sortedRows) {
            appendLine(
                listOf(
                    csvField(r.accountName),
                    r.date.format(DATE_FORMATTER),
                    csvField(r.totalWithdrawalAmounts.moneyComma()),
                    r.numberOfDeposits.toString(),
                    csvField(r.totalDepositAmounts.moneyComma()),
                    csvField(r.turnoverVelocity.moneyComma()),
                    r.riskFlagLevel
                ).joinToString(",")
            )
        }
    }
    outputFile.writeText(csvContent)

    return sortedRows
}

fun generateMonthlyCashFlowReport(
    transactions: List<BankTransaction>,
    outputFile: File = File("report4_monthly_cash_flow.csv")
): List<MonthlyCashFlowRow> {
    if (transactions.isEmpty()) return emptyList()


    val byMonth = transactions.groupBy { YearMonth.from(it.date) }
    val rows = mutableListOf<MonthlyCashFlowRow>()

    for ((month, monthTxns) in byMonth) {
        val totalDeposits = monthTxns
            .filter { it.type == "deposit" }
            .sumOf { it.amount }
            .roundMoney()

        val totalWithdrawals = monthTxns
            .filter { it.type == "withdrawal" }
            .sumOf { it.amount }
            .roundMoney()

        val netCashFlow = (totalDeposits - totalWithdrawals).roundMoney()

        rows.add(
            MonthlyCashFlowRow(
                month = month,
                transactionCount = monthTxns.size,
                totalDeposits = totalDeposits,
                totalWithdrawals = totalWithdrawals,
                netCashFlow = netCashFlow
            )
        )
    }


    val sortedRows = rows.sortedBy { it.month }


    val csvContent = buildString {
        appendLine("Month,TransactionCount,TotalDeposits,TotalWithdrawals,NetCashFlow")
        for (r in sortedRows) {
            appendLine(
                listOf(
                    r.month.toString(),
                    r.transactionCount.toString(),
                    csvField(r.totalDeposits.moneyComma()),
                    csvField(r.totalWithdrawals.moneyComma()),
                    csvField(r.netCashFlow.moneyComma())
                ).joinToString(",")
            )
        }
    }
    outputFile.writeText(csvContent)

    return sortedRows
}

fun generateSummaryJson(
    transactions: List<BankTransaction>,
    report2: List<InterestProjectionRow>,
    outputFile: File = File("summary.json")
): String {

    val byAccount = transactions.groupBy { it.accountNumber }
    val totalAccounts = byAccount.size

    val totalDeposits = transactions
        .filter { it.type == "deposit" }
        .sumOf { it.amount }
        .roundMoney()

    val totalWithdrawals = transactions
        .filter { it.type == "withdrawal" }
        .sumOf { it.amount }
        .roundMoney()


    val totalBalance = byAccount.values
        .sumOf { accountTxns ->
            accountTxns.latest().currentBalance
        }
        .roundMoney()


    val totalInterestExpense = report2
        .sumOf { it.expectedInterest }
        .roundMoney()


    val json = """
        {
          "total_accounts": $totalAccounts,
          "total_deposits": ${totalDeposits.money()},
          "total_withdrawals": ${totalWithdrawals.money()},
          "total_balance": ${totalBalance.money()},
          "total_interest_expense": ${totalInterestExpense.money()}
        }
    """.trimIndent()

    outputFile.writeText("$json\n")
    return json
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

fun promptAmountAtLeast(label: String, min: Double): Double {
    while (true) {
        val value = promptAmount(label)
        if (value >= min) return value
        println("Amount must be at least ${min.money()}.")
    }
}


fun Double.moneyComma(): String = "%,.2f".format(Locale.US, this)


fun csvField(value: String): String =
    if (value.contains(",") || value.contains("\"")) "\"${value.replace("\"", "\"\"")}\""
    else value


fun computeExpectedInterest(startingBalance: Double, days: Int = PROJECTION_DAYS): Double {
    val dailyRate = ANNUAL_INTEREST_RATE / DAYS_PER_YEAR
    var balance = startingBalance
    var totalInterest = 0.0

    for (day in 1..days) {
        val interest = (balance * dailyRate).roundMoney()
        totalInterest = (totalInterest + interest).roundMoney()
        balance = (balance + interest).roundMoney()
    }
    return totalInterest
}

// ---------------------------------------------------------------------------
// Transactions
// ---------------------------------------------------------------------------

interface Transaction {

    val returnsToMenuAutomatically: Boolean get() = false

    fun execute()
}


class RegisterAccount(private val store: AccountStore) : Transaction {
    override val returnsToMenuAutomatically = true

    override fun execute() {
        do {
            println("Register Account")
            var name: String
            while (true) {
                name = promptNonBlank("Account Name (First name Last name): ").replace(Regex("\\s+"), " ")
                when {
                    name.contains(",") || name.contains("\"") -> println("Name must not contain commas or quotes.")
                    name.split(" ").size < 2 -> println("Enter both a first name and a last name.")
                    store.accounts.values.any { it.name.equals(name, ignoreCase = true) } ->
                        println("An account with that name already exists.")
                    else -> break
                }
            }
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
                    println("Outputs saved to individual files...")
                    println()

                    val report1 = generatePrivilegeTierReport(data)
                    printPrivilegeTierPreview(report1)
                    val report2 = generateInterestProjectionReport(data)
                    printInterestProjectionPreview(report2)
                    val report3 = generateSuspiciousBehaviorReport(data)
                    printSuspiciousBehaviorPreview(report3)
                    val report4 = generateMonthlyCashFlowReport(data)
                    printMonthlyCashFlowPreview(report4)

                    val summaryJson = generateSummaryJson(data, report2)
                    println("Summary State (summary.json):")
                    println(summaryJson)
                    println()
                }
                3 -> return
            }
        }
    }

    fun printPrivilegeTierPreview(rows: List<PrivilegeTierRow>) {
        println("Report 1: Banking Privilege Tiers")
        println("%-22s | %-10s | %-14s | %-19s | %s".format(
            Locale.US, "AccountName", "AccountAge", "CurrentBalance", "AverageDailyBalance", "PrivilegeTier"
        ))
        rows.take(2).forEach { r ->
            println("%-22s | %-10d | %-14s | %-19s | %s".format(
                Locale.US,
                r.accountName,
                r.accountAge,
                r.currentBalance.moneyComma(),
                r.averageDailyBalance.moneyComma(),
                r.privilegeTier
            ))
        }
        println("(Full table exported to report1_banking_privilege_tiers.csv)")
        println()
    }

    fun printInterestProjectionPreview(rows: List<InterestProjectionRow>) {
        println("Report 2: Interest Expense Projection")
        println("%-22s | %-14s | %s".format(
            Locale.US, "AccountName", "CurrentBalance", "ExpectedInterest"
        ))
        rows.take(2).forEach { r ->
            println("%-22s | %-14s | %s".format(
                Locale.US,
                r.accountName,
                r.currentBalance.moneyComma(),
                r.expectedInterest.moneyComma()
            ))
        }
        println("(Full table exported to report2_interest_expense_projection.csv)")
        println()
    }

    fun printSuspiciousBehaviorPreview(rows: List<SuspiciousBehaviorRow>) {
        println("Report 3: Suspicious Client Behavior")
        println("%-20s | %-10s | %-15s | %-8s | %-15s | %-10s | %s".format(
            Locale.US, "AccountName", "Date", "TotalWithdrawal", "Deposits", "TotalDeposit", "Velocity%", "RiskFlag"
        ))
        rows.take(2).forEach { r ->
            println("%-20s | %-10s | %-15s | %-8d | %-15s | %-10s | %s".format(
                Locale.US,
                r.accountName,
                r.date.format(DATE_FORMATTER),
                r.totalWithdrawalAmounts.moneyComma(),
                r.numberOfDeposits,
                r.totalDepositAmounts.moneyComma(),
                r.turnoverVelocity.moneyComma(),
                r.riskFlagLevel
            ))
        }
        println("(Full table exported to report3_suspicious_client_behavior.csv)")
        println()
    }
    fun printMonthlyCashFlowPreview(rows: List<MonthlyCashFlowRow>) {
        println("Report 4: Monthly Cash Flow Summary")
        println("%-8s | %-12s | %-16s | %-16s | %s".format(
            Locale.US, "Month", "Transactions", "TotalDeposits", "TotalWithdrawals", "NetCashFlow"
        ))
        rows.take(2).forEach { r ->
            println("%-8s | %-12d | %-16s | %-16s | %s".format(
                Locale.US,
                r.month.toString(),
                r.transactionCount,
                r.totalDeposits.moneyComma(),
                r.totalWithdrawals.moneyComma(),
                r.netCashFlow.moneyComma()
            ))
        }
        println("(Full table exported to report4_monthly_cash_flow.csv)")
        println()
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
