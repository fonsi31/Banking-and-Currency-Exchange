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
    "Chinese Yuan Renminni (CNY)"
)

class UserChoice {
    fun displayChoice(): Int {
        print("Choice: ")
        val number = readln().toInt()
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
        val currentBalance = 1000.00   // REQ-0009 default
        val currency = "PHP"           // REQ-0009 default

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
        val currentBalance = 1000.00   // REQ-0013 default
        val currency = "PHP"           // REQ-0013 default

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
        val selection = readln().toInt()

        print("Exchange Rate: ")
        val rate = readln().toDouble()

        println()
        println("***")
        println("Select Foreign Currency = [$selection]")
        println("Exchange Rate = %.2f".format(rate))
    }
}

class CurrencyExchange {
    // Index matches the currencies list: PHP, USD, JPY, GBP, EUR, CNY
    private val rates = listOf(1.00, 62.00, 0.40, 84.00, 72.00, 9.00)

    fun execute() {
        println("Foreign Currency Exchange")

        print("Source Amount (PHP): ")
        val sourceAmount = readln().toDouble()

        println()
        println("Exchanged Currency")
        currencies.forEachIndexed { i, name ->
            println("[${i + 1}] $name = %.2f".format(sourceAmount * rates[i]))
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

    val selection = UserChoice()
    val choice = selection.displayChoice()

    println()

    when (choice) {
        1 -> RegisterAccount().execute()
        2 -> Deposit().execute()
        3 -> Withdraw().execute()
        4 -> CurrencyExchange().execute()
        5 -> RecordExchangeRate().execute()
        6 -> {}   // Show Interest Amount has no requirements in this milestone
        else -> println("Invalid option selected.")
    }
}
