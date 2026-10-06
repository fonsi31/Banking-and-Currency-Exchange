/********************
Last names: Baun
Language: Kotlin
Paradigm(s): Multi-Paradigm
********************/

val currencies = listOf(
    "Philippine Peso (PHP)",
    "United States Dollar (USD)",
    "Japanese Yen (JPY)",
    "British Pound Sterling (GBP)",
    "Euro (EUR)",
    "Chinese Yuan (CNY)"
)

// Exchange rates to PHP, same order as the currencies list: PHP, USD, JPY, GBP, EUR, CNY
val exchangeRates = listOf(1.00, 62.00, 0.40, 84.00, 72.00, 9.00)


fun readSelection(): Int = readln().trim().removeSurrounding("[", "]").toInt()

// Separator printed before each module
fun printSeparator() {
    println("=".repeat(38))
    println()
}

class UserChoice {
    fun displayChoice(): Int {
        print("Choice: ")
        val number = readSelection()
        println()
        println("***")
        println("Choice = $number")
        return number
    }
}

class RegisterAccount {
    fun execute() {
        println("Register Account Name")

        print("Account Name: ")
        val accountName = readln()

        println()
        println("***")

        println("Account Name = $accountName")
    }
}

class Deposit {
    fun execute() {
        val currentBalance = 1000.00
        val currency = "PHP"

        println("Deposit Amount")

        print("Account Name: ")
        val accountName = readln()

        println("Current Balance: %.2f".format(currentBalance))
        println("Currency: $currency")
        println()

        print("Deposit Amount: ")
        val depositAmount = readln().toDouble()

        println()
        println("***")

        println("Account Name = $accountName")
        println("Deposit Amount = %.2f".format(depositAmount))
    }
}

class Withdraw {
    fun execute() {
        val currentBalance = 1000.00
        val currency = "PHP"

        println("Withdraw Amount")

        print("Account Name: ")
        val accountName = readln()

        println("Current Balance: %.2f".format(currentBalance))
        println("Currency: $currency")
        println()

        print("Withdraw Amount: ")
        val withdrawAmount = readln().toDouble()

        println()
        println("***")

        println("Account Name = $accountName")
        println("Withdraw Amount = %.2f".format(withdrawAmount))
    }
}

class RecordExchangeRate {
    fun execute() {
        println("Record Exchange Rate")
        println()

        currencies.forEachIndexed { i, name ->
            println("[${i + 1}] $name")
        }
        println()

        print("Select Foreign Currency: ")
        val selection = readSelection()

        if (selection !in 1..currencies.size) {
            println()
            println("Invalid option selected.")
            return
        }

        val rate = exchangeRates[selection - 1]
        println("Exchange Rate: %.2f".format(rate))

        println()
        println("***")
        println("Select Foreign Currency = [$selection]")
        println("Exchange Rate = %.2f".format(rate))
    }
}

class CurrencyExchange {
    fun execute() {
        println("Foreign Currency Exchange")

        print("Source Amount (PHP): ")
        val sourceAmount = readln().toDouble()

        println()
        println("Exchanged Currency")
        currencies.forEachIndexed { i, name ->
            println("[${i + 1}] $name = %.2f".format(sourceAmount * exchangeRates[i]))
        }

        println()
        println("***")
        println("Source Currency = ${currencies[0]}")
        println("Source Amount (PHP) = %.2f".format(sourceAmount))
    }
}

fun main() {

    println("Select Transaction:")
    println("[1] Register Account Name")
    println("[2] Deposit Amount")
    println("[3] Withdraw Amount")
    println("[4] Currency Exchange")
    println("[5] Record Exchange Rates")
    println("[6] Show Interest Amount")
    println()


    UserChoice().displayChoice()
    println()


    printSeparator()
    RegisterAccount().execute()
    println()

    printSeparator()
    Deposit().execute()
    println()

    printSeparator()
    Withdraw().execute()
    println()

    printSeparator()
    RecordExchangeRate().execute()
    println()

    printSeparator()
    CurrencyExchange().execute()
}
