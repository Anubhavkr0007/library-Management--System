package dao;

import database.DBConnection;
import model.Member;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MemberDAO {

    public void addMember(Member m) throws SQLException {
        String sql = "INSERT INTO members(name, email, phone) VALUES (?, ?, ?)";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getPhone());
            ps.executeUpdate();
        }
    }

    public void updateMember(Member m) throws SQLException {
        String sql = "UPDATE members SET name=?, email=?, phone=? WHERE id=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getEmail());
            ps.setString(3, m.getPhone());
            ps.setInt(4, m.getId());
            if (ps.executeUpdate() == 0) {
                throw new IllegalArgumentException("Member not found with ID " + m.getId());
            }
        }
    }

    public List<Member> searchMember(String keyword) throws SQLException {
        String sql = "SELECT * FROM members WHERE name LIKE ? OR email LIKE ? ORDER BY id";
        List<Member> list = new ArrayList<>();
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

    public List<Member> viewMembers() throws SQLException {
        List<Member> list = new ArrayList<>();
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM members ORDER BY id")) {
            while (rs.next()) list.add(map(rs));
        }
        return list;
    }

    public int countMembers() throws SQLException {
        try (Connection c = DBConnection.getConnection();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT COUNT(*) FROM members")) {
            rs.next();
            return rs.getInt(1);
        }
    }

    // Member login: email + phone
    public Member login(String email, String phone) throws SQLException {
        String sql = "SELECT * FROM members WHERE email=? AND phone=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, phone);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    private Member map(ResultSet rs) throws SQLException {
        return new Member(rs.getInt("id"), rs.getString("name"), rs.getString("email"), rs.getString("phone"));
    }
}
