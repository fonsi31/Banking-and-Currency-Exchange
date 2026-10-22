/********************
Last names: Baun, Cauilan, Dimakuta, Marcelino
Language: Kotlin
Paradigm(s): Multi-Paradigm
 ********************/

import java.math.BigDecimal
import java.math.RoundingMode
import java.util.Locale
import kotlin.system.exitProcess

// ---------------------------------------------------------------------------
// Constants and data
// ---------------------------------------------------------------------------

const val ANNUAL_INTEREST_RATE = 0.05   // 5% per annum
const val DAYS_PER_YEAR = 365
const val BASE_CURRENCY_INDEX = 0       // PHP is the base currency
const val STARTING_BALANCE = 1000.00

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

/** The single account used by the session. The balance is always stored in the base currency (PHP). */
class Account {
    var name: String? = null
    var balance: Double = STARTING_BALANCE
    val currency: Currency = baseCurrency
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

/** Prints [label], then returns the trimmed line typed by the user. Exits cleanly if input ends. */
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

/** Accepts "2" or "[2]" and keeps asking until the value is inside [range]. */
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

// ---------------------------------------------------------------------------
// Transactions (one class per menu option)
// ---------------------------------------------------------------------------

interface Transaction {
    /** If true, the app goes straight back to the menu instead of asking "Back to the Main Menu (Y/N)". */
    val returnsToMenuAutomatically: Boolean get() = false

    fun execute()
}


class RegisterAccount(private val account: Account) : Transaction {
    override fun execute() {
        println("Register Account Name")
        account.name = promptNonBlank("Account Name: ")
    }
}


class Deposit(private val account: Account) : Transaction {
    override fun execute() {
        println("Deposit Amount")
        if (!account.requireRegistered()) return

        println("Account Name: ${account.name}")
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
        println("Updated Balance: ${account.balance.money()}")
    }
}


class Withdraw(private val account: Account) : Transaction {
    override fun execute() {
        println("Withdraw Amount")
        if (!account.requireRegistered()) return

        println("Account Name: ${account.name}")
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
            return
        }
        account.balance = (account.balance - debited).roundMoney()
        println("Updated Balance: ${account.balance.money()}")
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


class ShowInterestAmount(private val account: Account) : Transaction {
    override fun execute() {
        println("Show Interest Amount")
        if (!account.requireRegistered()) return

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

// ---------------------------------------------------------------------------
// Main menu
// ---------------------------------------------------------------------------

fun printMenu() {
    println("Select Transaction:")
    println("[1] Register Account Name")
    println("[2] Deposit Amount")
    println("[3] Withdraw Amount")
    println("[4] Currency Exchange")
    println("[5] Record Exchange Rates")
    println("[6] Show Interest Amount")
}

fun main() {
    val account = Account()
    val transactions: Map<Int, Transaction> = mapOf(
        1 to RegisterAccount(account),
        2 to Deposit(account),
        3 to Withdraw(account),
        4 to CurrencyExchange(),
        5 to RecordExchangeRate(),
        6 to ShowInterestAmount(account),
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
