package dao;

import database.DBConnection;
import exception.BookNotFoundException;
import model.IssueRecord;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class IssueDAO {

    public static final int LOAN_DAYS = 14; // due date = issue date + 14 days

    // Member selects book -> check availability -> save issue record -> reduce quantity
    public IssueRecord issueBook(int bookId, int memberId) throws SQLException, BookNotFoundException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                int qty;
                try (PreparedStatement ps = c.prepareStatement("SELECT quantity FROM books WHERE id=? FOR UPDATE")) {
                    ps.setInt(1, bookId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new BookNotFoundException("Book not found with ID " + bookId);
                        qty = rs.getInt(1);
                    }
                }
                if (qty <= 0) throw new IllegalStateException("Book is not available right now");

                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM members WHERE id=?")) {
                    ps.setInt(1, memberId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("Member not found with ID " + memberId);
                    }
                }

                LocalDate issue = LocalDate.now();
                LocalDate due = issue.plusDays(LOAN_DAYS);
                int newId;
                try (PreparedStatement ps = c.prepareStatement(
                        "INSERT INTO issue_records(book_id, member_id, issue_date, due_date) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, bookId);
                    ps.setInt(2, memberId);
                    ps.setDate(3, Date.valueOf(issue));
                    ps.setDate(4, Date.valueOf(due));
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        newId = keys.getInt(1);
                    }
                }

                try (PreparedStatement ps = c.prepareStatement("UPDATE books SET quantity = quantity - 1 WHERE id=?")) {
                    ps.setInt(1, bookId);
                    ps.executeUpdate();
                }
                c.commit();
                return new IssueRecord(newId, bookId, memberId, issue, due, null);
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    // Return request -> update return date -> increase quantity
    public IssueRecord returnBook(int issueId) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                IssueRecord r;
                try (PreparedStatement ps = c.prepareStatement(
                        "SELECT * FROM issue_records WHERE id=? AND return_date IS NULL FOR UPDATE")) {
                    ps.setInt(1, issueId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) throw new IllegalArgumentException("No active issue record with ID " + issueId);
                        r = map(rs);
                    }
                }
                LocalDate today = LocalDate.now();
                try (PreparedStatement ps = c.prepareStatement("UPDATE issue_records SET return_date=? WHERE id=?")) {
                    ps.setDate(1, Date.valueOf(today));
                    ps.setInt(2, issueId);
                    ps.executeUpdate();
                }
                try (PreparedStatement ps = c.prepareStatement("UPDATE books SET quantity = quantity + 1 WHERE id=?")) {
                    ps.setInt(1, r.getBookId());
                    ps.executeUpdate();
                }
                c.commit();
                r.setReturnDate(today);
                return r;
            } catch (Exception e) {
                c.rollback();
                throw e;
            }
        }
    }

    public List<IssueRecord> viewIssues() throws SQLException {
        List<IssueRecord> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM issue_records ORDER BY id DESC")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countIssued() throws SQLException {
        return count("SELECT COUNT(*) FROM issue_records WHERE return_date IS NULL");
    }

    public int countReturned() throws SQLException {
        return count("SELECT COUNT(*) FROM issue_records WHERE return_date IS NOT NULL");
    }

    private int count(String sql) throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private IssueRecord map(ResultSet rs) throws SQLException {
        Date ret = rs.getDate("return_date");
        return new IssueRecord(
                rs.getInt("id"), rs.getInt("book_id"), rs.getInt("member_id"),
                rs.getDate("issue_date").toLocalDate(), rs.getDate("due_date").toLocalDate(),
                ret == null ? null : ret.toLocalDate());
    }
}
