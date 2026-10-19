import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Scanner;

// ---------- 1. Custom exception ----------
class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

// ---------- 2. Database connection ----------
class DBConnection {
    private static final String URL = "jdbc:mysql://localhost:3306/bankdb";
    private static final String USER = "root";
    private static final String PASSWORD = "akhileshanand@2007";

    public static Connection getConnection() throws Exception {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}

// ---------- 3. Abstract class (encapsulation + abstraction) ----------
abstract class Account {
    private int accountNo;
    private String holderName;
    private double balance;

    public Account(int accountNo, String holderName, double balance) {
        this.accountNo = accountNo;
        this.holderName = holderName;
        this.balance = balance;
    }

    public int getAccountNo() { return accountNo; }
    public String getHolderName() { return holderName; }
    public double getBalance() { return balance; }
    public void setBalance(double balance) { this.balance = balance; }

    public abstract double getMinBalance();
    public abstract String getType();
}

// ---------- 4. Inheritance ----------
class SavingsAccount extends Account {
    public SavingsAccount(int accountNo, String holderName, double balance) {
        super(accountNo, holderName, balance);
    }
    @Override
    public double getMinBalance() { return 500; }
    @Override
    public String getType() { return "Savings"; }
}

class CurrentAccount extends Account {
    public CurrentAccount(int accountNo, String holderName, double balance) {
        super(accountNo, holderName, balance);
    }
    @Override
    public double getMinBalance() { return 0; }
    @Override
    public String getType() { return "Current"; }
}

// ---------- 5. All database operations ----------
class AccountDAO {

    public int createAccount(String name, String type, double amount) throws Exception {
        Account temp;
        if (type.equals("Savings")) {
            temp = new SavingsAccount(0, name, amount);
        } else {
            temp = new CurrentAccount(0, name, amount);
        }
        if (amount < temp.getMinBalance()) {
            throw new InsufficientBalanceException(
                "Minimum opening balance for " + type + " account is " + temp.getMinBalance());
        }

        String sql = "INSERT INTO accounts (holder_name, account_type, balance) VALUES (?, ?, ?)";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, name);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int accNo = rs.getInt(1);
            logTransaction(con, accNo, "Opening", amount);
            return accNo;
        }
    }

    public void deposit(int accNo, double amount) throws Exception {
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        try (Connection con = DBConnection.getConnection()) {
            if (getAccount(con, accNo) == null) {
                throw new Exception("Account not found");
            }
            updateBalance(con, accNo, amount);
            logTransaction(con, accNo, "Deposit", amount);
        }
    }

    public void withdraw(int accNo, double amount) throws Exception {
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        try (Connection con = DBConnection.getConnection()) {
            Account acc = getAccount(con, accNo);
            if (acc == null) {
                throw new Exception("Account not found");
            }
            if (acc.getBalance() - amount < acc.getMinBalance()) {
                throw new InsufficientBalanceException(
                    "Insufficient balance. Minimum balance to maintain: " + acc.getMinBalance());
            }
            updateBalance(con, accNo, -amount);
            logTransaction(con, accNo, "Withdraw", amount);
        }
    }

    public void transfer(int fromAcc, int toAcc, double amount) throws Exception {
        if (amount <= 0) {
            throw new Exception("Amount must be greater than 0");
        }
        if (fromAcc == toAcc) {
            throw new Exception("Cannot transfer to the same account");
        }
        try (Connection con = DBConnection.getConnection()) {
            try {
                con.setAutoCommit(false);

                Account from = getAccount(con, fromAcc);
                Account to = getAccount(con, toAcc);
                if (from == null || to == null) {
                    throw new Exception("Account not found");
                }
                if (from.getBalance() - amount < from.getMinBalance()) {
                    throw new InsufficientBalanceException(
                        "Insufficient balance. Minimum balance to maintain: " + from.getMinBalance());
                }

                updateBalance(con, fromAcc, -amount);
                updateBalance(con, toAcc, amount);
                logTransaction(con, fromAcc, "Transfer Out", amount);
                logTransaction(con, toAcc, "Transfer In", amount);

                con.commit();
            } catch (Exception e) {
                con.rollback();
                throw e;
            }
        }
    }

    public void viewBalance(int accNo) throws Exception {
        try (Connection con = DBConnection.getConnection()) {
            Account acc = getAccount(con, accNo);
            if (acc == null) {
                throw new Exception("Account not found");
            }
            System.out.println("Account No : " + acc.getAccountNo());
            System.out.println("Name       : " + acc.getHolderName());
            System.out.println("Type       : " + acc.getType());
            System.out.println("Balance    : " + acc.getBalance());
        }
    }

    public void showTransactions(int accNo) throws Exception {
        String sql = "SELECT txn_id, txn_type, amount, txn_date FROM transactions "
                   + "WHERE account_no = ? ORDER BY txn_date DESC, txn_id DESC";
        try (Connection con = DBConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accNo);
            ResultSet rs = ps.executeQuery();

            boolean found = false;
            System.out.println("ID | Type | Amount | Date");
            while (rs.next()) {
                found = true;
                System.out.println(rs.getInt("txn_id") + " | "
                        + rs.getString("txn_type") + " | "
                        + rs.getDouble("amount") + " | "
                        + rs.getTimestamp("txn_date"));
            }
            if (!found) {
                System.out.println("No transactions found.");
            }
        }
    }

    // ----- helper methods -----
    public Account getAccount(Connection con, int accNo) throws Exception {
        String sql = "SELECT * FROM accounts WHERE account_no = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accNo);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String name = rs.getString("holder_name");
                String type = rs.getString("account_type");
                double bal = rs.getDouble("balance");
                if (type.equals("Savings")) {
                    return new SavingsAccount(accNo, name, bal);
                } else {
                    return new CurrentAccount(accNo, name, bal);
                }
            }
            return null;
        }
    }

    private void updateBalance(Connection con, int accNo, double change) throws Exception {
        String sql = "UPDATE accounts SET balance = balance + ? WHERE account_no = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setDouble(1, change);
            ps.setInt(2, accNo);
            ps.executeUpdate();
        }
    }

    private void logTransaction(Connection con, int accNo, String type, double amount) throws Exception {
        String sql = "INSERT INTO transactions (account_no, txn_type, amount) VALUES (?, ?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, accNo);
            ps.setString(2, type);
            ps.setDouble(3, amount);
            ps.executeUpdate();
        }
    }
}

// ---------- 6. Main class with the menu (the only public class) ----------
public class Bankdb {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AccountDAO dao = new AccountDAO();
        int choice = 0;

        do {
            System.out.println("\n===== BANK MENU =====");
            System.out.println("1. Create Account");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer");
            System.out.println("5. Check Balance");
            System.out.println("6. Transaction History");
            System.out.println("7. Exit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine());

                switch (choice) {
                    case 1: {
                        System.out.print("Holder name: ");
                        String name = sc.nextLine();
                        System.out.print("Account type (1 = Savings, 2 = Current): ");
                        int t = Integer.parseInt(sc.nextLine());
                        String type = (t == 1) ? "Savings" : "Current";
                        System.out.print("Opening amount: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        int accNo = dao.createAccount(name, type, amt);
                        System.out.println("Account created! Your account number is " + accNo);
                        break;
                    }
                    case 2: {
                        System.out.print("Account number: ");
                        int accNo = Integer.parseInt(sc.nextLine());
                        System.out.print("Amount to deposit: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        dao.deposit(accNo, amt);
                        System.out.println("Deposit successful.");
                        break;
                    }
                    case 3: {
                        System.out.print("Account number: ");
                        int accNo = Integer.parseInt(sc.nextLine());
                        System.out.print("Amount to withdraw: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        dao.withdraw(accNo, amt);
                        System.out.println("Withdrawal successful.");
                        break;
                    }
                    case 4: {
                        System.out.print("From account number: ");
                        int from = Integer.parseInt(sc.nextLine());
                        System.out.print("To account number: ");
                        int to = Integer.parseInt(sc.nextLine());
                        System.out.print("Amount to transfer: ");
                        double amt = Double.parseDouble(sc.nextLine());
                        dao.transfer(from, to, amt);
                        System.out.println("Transfer successful.");
                        break;
                    }
                    case 5: {
                        System.out.print("Account number: ");
                        int accNo = Integer.parseInt(sc.nextLine());
                        dao.viewBalance(accNo);
                        break;
                    }
                    case 6: {
                        System.out.print("Account number: ");
                        int accNo = Integer.parseInt(sc.nextLine());
                        dao.showTransactions(accNo);
                        break;
                    }
                    case 7:
                        System.out.println("Thank you for banking with us!");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }
            } catch (NumberFormatException e) {
                System.out.println("Error: Please enter a valid number.");
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage());
            }
        } while (choice != 7);
    }
}