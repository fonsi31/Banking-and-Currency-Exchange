# Banking and Currency Exchange Application

A CLI banking and currency application programmed in C, Java, Kotlin, and R.

## Key Features

- Stores user/account data (Account name and balance).
- Allows a user to deposit and withdraw.
- Convert between supported currencies using their recorded exchange rates.
- Manually set the exchange rate of a currency against PHP.
- Displays projected daily balances and interest for a user-specified number of days.

### R requirements and Setup

- Version: R 4.6.1
- IDE: Visual Studio Code
- Extension: R by REditorSupport
- Dependency: jsonlite package

#### How to run

1. Clone the repo
   
3. Open a terminal
  
4. navigate to the project folder

```bash
cd Banking and Currency Exchange\R
```

4. Activate interactive R console

```bash
R
```

5. Run the program
```bash
source("main.R")
```

#### Package Installation

If jsonlite isn't installed, run the following command in R:

```bash
install.packages("jsonlite")
```
