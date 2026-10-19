# Bank-Management-System
# Bank Account Management System

I built this as part of my college project in my 3rd semester (second year), together with my friend. It's a simple console-based bank where you can create accounts, move money around and see what happened to it later. The code is written in Java and the data lives in a MySQL database.

I wanted to see how a Java program actually talks to a real database, and a bank felt like a good example, because money has rules and I got to practise those with OOP.

## What it can do
- Create a Savings or Current account
- Deposit and withdraw money
- Transfer money between two accounts
- Check the balance of any account
- See the full transaction history of an account

The menu looks like this:

    1. Create Account
    2. Deposit
    3. Withdraw
    4. Transfer
    5. Check Balance
    6. Transaction History
    7. Exit

## How the rules work
- A **Savings** account has to keep a minimum balance of 500.
- A **Current** account can go down to 0.
- You can't deposit or withdraw a zero or negative amount.
- If something goes wrong in a transfer, nothing changes. Either both accounts are updated or neither is.

## OOP concepts I used
- **Encapsulation:** the account details are private and only reachable through getters and setters
- **Abstraction:** `Account` is an abstract class that says every account needs a minimum balance and a type
- **Inheritance:** `SavingsAccount` and `CurrentAccount` extend `Account`
- **Polymorphism:** each account type gives its own minimum balance, and the program picks the right one at runtime
- **Exception handling:** a custom `InsufficientBalanceException` handles withdrawals that break the rules

## Database
Two tables in a database called `bankdb`:
- `accounts`: account number, holder name, account type, balance, created date
- `transactions`: transaction id, account number, type, amount, date

Each transaction is linked to its account with a foreign key. The SQL file also has a few report queries (total money in the bank, richest account, transactions per account, and a JOIN that shows names next to each transaction).

## Built with
Java, JDBC, MySQL, MySQL Workbench and NetBeans

## How to run it
1. Open MySQL Workbench and run the setup SQL file from this repository (**Bank.sql**) to create the database and tables.
2. Download MySQL Connector/J and add the jar to the project libraries.
3. Open the `DBConnection` class and put your own MySQL password in.
4. Run `Bankdb.java`. In NetBeans, right-click the file and choose **Run File**.

Don't run the setup script again after you've added data, because it can reset the tables.

## Things I learned
- Using `PreparedStatement` instead of building SQL strings by hand
- Why transfers need `commit()` and `rollback()`, so money never disappears halfway
- Setting up a JDBC driver, which took longer than I expected (my first errors were "No suitable driver" and "Access denied")
- Reading error messages properly instead of guessing

## What I'd improve next
- Add a login with a PIN
- Use `BigDecimal` instead of `double` for money
- Split the code into separate files
- Build a simple GUI

Made by Akhilesh Anand
