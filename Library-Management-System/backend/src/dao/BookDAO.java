package dao;

import database.DBConnection;
import exception.BookNotFoundException;
import model.Book;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class BookDAO {

    public void addBook(Book b) throws SQLException {
        String sql = "INSERT INTO books(title, author, quantity) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setInt(3, b.getQuantity());
            ps.executeUpdate();
        }
    }

    public void updateBook(Book b) throws SQLException, BookNotFoundException {
        String sql = "UPDATE books SET title=?, author=?, quantity=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, b.getTitle());
            ps.setString(2, b.getAuthor());
            ps.setInt(3, b.getQuantity());
            ps.setInt(4, b.getId());
            if (ps.executeUpdate() == 0) {
                throw new BookNotFoundException("Book not found with ID " + b.getId());
            }
        }
    }

    public List<Book> searchBook(String keyword) throws SQLException {
        String sql = "SELECT * FROM books WHERE title LIKE ? OR author LIKE ? ORDER BY id";
        List<Book> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + keyword + "%");
            ps.setString(2, "%" + keyword + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(map(rs));
            }
        }
        return list;
    }

    public List<Book> viewBooks() throws SQLException {
        List<Book> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM books ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countBooks() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM books")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    private Book map(ResultSet rs) throws SQLException {
        return new Book(rs.getInt("id"), rs.getString("title"), rs.getString("author"), rs.getInt("quantity"));
    }
}
