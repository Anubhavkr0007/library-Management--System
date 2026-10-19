import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import dao.BookDAO;
import dao.IssueDAO;
import dao.MemberDAO;
import database.DBConnection;
import exception.BookNotFoundException;
import model.Admin;
import model.Book;
import model.IssueRecord;
import model.Member;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entry point. Starts a small HTTP server (JDK built-in) on port 8080
 * so the HTML/CSS/JS frontend can talk to the DAO classes.
 */
public class Main {

    private static final BookDAO bookDAO = new BookDAO();
    private static final MemberDAO memberDAO = new MemberDAO();
    private static final IssueDAO issueDAO = new IssueDAO();

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/api/", Main::handle);
        server.start();
        System.out.println("Library backend running at http://localhost:8080/api/");
    }

    private static void handle(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        ex.getResponseHeaders().add("Content-Type", "application/json; charset=UTF-8");

        if (ex.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
            ex.sendResponseHeaders(204, -1);
            return;
        }

        String raw = ex.getRequestMethod().equalsIgnoreCase("GET")
                ? ex.getRequestURI().getRawQuery()
                : new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        Map<String, String> p = parse(raw);
        String path = ex.getRequestURI().getPath();

        String out;
        try {
            out = route(path, p);
        } catch (BookNotFoundException e) {
            out = fail(e.getMessage());
        } catch (NumberFormatException e) {
            out = fail("Please enter valid numbers");
        } catch (Exception e) {
            out = fail(e.getMessage());
        }

        byte[] bytes = out.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(200, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String route(String path, Map<String, String> p) throws Exception {
        switch (path) {
            case "/api/login": {
                String role = p.getOrDefault("role", "admin");
                if (role.equals("admin")) {
                    Admin a = loginAdmin(p.get("username"), p.get("password"));
                    return a == null ? fail("Invalid admin username or password")
                            : "{\"ok\":true,\"role\":\"admin\",\"name\":" + q(a.getUsername()) + "}";
                }
                Member m = memberDAO.login(p.get("username"), p.get("password"));
                return m == null ? fail("Invalid member email or phone")
                        : "{\"ok\":true,\"role\":\"member\",\"name\":" + q(m.getName()) + "}";
            }
            case "/api/dashboard":
                return "{\"ok\":true,\"books\":" + bookDAO.countBooks()
                        + ",\"members\":" + memberDAO.countMembers()
                        + ",\"issued\":" + issueDAO.countIssued()
                        + ",\"returned\":" + issueDAO.countReturned() + "}";

            case "/api/books": {
                String kw = p.getOrDefault("q", "").trim();
                List<Book> list = kw.isEmpty() ? bookDAO.viewBooks() : bookDAO.searchBook(kw);
                StringBuilder sb = new StringBuilder("{\"ok\":true,\"data\":[");
                for (int i = 0; i < list.size(); i++) {
                    Book b = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"id\":").append(b.getId())
                      .append(",\"title\":").append(q(b.getTitle()))
                      .append(",\"author\":").append(q(b.getAuthor()))
                      .append(",\"quantity\":").append(b.getQuantity()).append("}");
                }
                return sb.append("]}").toString();
            }
            case "/api/books/add":
                bookDAO.addBook(new Book(0, need(p, "title"), need(p, "author"), Integer.parseInt(need(p, "quantity"))));
                return ok("Book added");
            case "/api/books/update":
                bookDAO.updateBook(new Book(Integer.parseInt(need(p, "id")), need(p, "title"), need(p, "author"),
                        Integer.parseInt(need(p, "quantity"))));
                return ok("Book updated");

            case "/api/members": {
                String kw = p.getOrDefault("q", "").trim();
                List<Member> list = kw.isEmpty() ? memberDAO.viewMembers() : memberDAO.searchMember(kw);
                StringBuilder sb = new StringBuilder("{\"ok\":true,\"data\":[");
                for (int i = 0; i < list.size(); i++) {
                    Member m = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"id\":").append(m.getId())
                      .append(",\"name\":").append(q(m.getName()))
                      .append(",\"email\":").append(q(m.getEmail()))
                      .append(",\"phone\":").append(q(m.getPhone())).append("}");
                }
                return sb.append("]}").toString();
            }
            case "/api/members/add":
                memberDAO.addMember(new Member(0, need(p, "name"), need(p, "email"), need(p, "phone")));
                return ok("Member added");
            case "/api/members/update":
                memberDAO.updateMember(new Member(Integer.parseInt(need(p, "id")), need(p, "name"),
                        need(p, "email"), need(p, "phone")));
                return ok("Member updated");

            case "/api/issue": {
                IssueRecord r = issueDAO.issueBook(Integer.parseInt(need(p, "bookId")), Integer.parseInt(need(p, "memberId")));
                return "{\"ok\":true,\"message\":" + q("Book issued. Issue ID " + r.getId() + ", due on " + r.getDueDate()) + "}";
            }
            case "/api/return": {
                IssueRecord r = issueDAO.returnBook(Integer.parseInt(need(p, "issueId")));
                String msg = r.isLate()
                        ? "Book returned LATE by " + r.getDaysLate() + " day(s)"
                        : "Book returned on time";
                return ok(msg);
            }
            case "/api/issues": {
                List<IssueRecord> list = issueDAO.viewIssues();
                StringBuilder sb = new StringBuilder("{\"ok\":true,\"data\":[");
                for (int i = 0; i < list.size(); i++) {
                    IssueRecord r = list.get(i);
                    if (i > 0) sb.append(",");
                    sb.append("{\"id\":").append(r.getId())
                      .append(",\"bookId\":").append(r.getBookId())
                      .append(",\"memberId\":").append(r.getMemberId())
                      .append(",\"issueDate\":").append(q(r.getIssueDate().toString()))
                      .append(",\"dueDate\":").append(q(r.getDueDate().toString()))
                      .append(",\"returnDate\":").append(r.getReturnDate() == null ? "null" : q(r.getReturnDate().toString()))
                      .append(",\"late\":").append(r.isLate()).append("}");
                }
                return sb.append("]}").toString();
            }
            default:
                return fail("Unknown request");
        }
    }

    private static Admin loginAdmin(String username, String password) throws Exception {
        String sql = "SELECT id FROM admin WHERE username=? AND password=?";
        try (Connection c = DBConnection.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.setString(2, password);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new Admin(rs.getInt(1), username, password) : null;
            }
        }
    }

    // ---- small helpers ----
    private static String need(Map<String, String> p, String key) {
        String v = p.get(key);
        if (v == null || v.trim().isEmpty()) throw new IllegalArgumentException("Please fill: " + key);
        return v.trim();
    }

    private static Map<String, String> parse(String s) {
        Map<String, String> map = new HashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String pair : s.split("&")) {
            String[] kv = pair.split("=", 2);
            String k = URLDecoder.decode(kv[0], StandardCharsets.UTF_8);
            String v = kv.length > 1 ? URLDecoder.decode(kv[1], StandardCharsets.UTF_8) : "";
            map.put(k, v);
        }
        return map;
    }

    private static String q(String s) {
        if (s == null) return "null";
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ") + "\"";
    }

    private static String ok(String msg) { return "{\"ok\":true,\"message\":" + q(msg) + "}"; }
    private static String fail(String msg) { return "{\"ok\":false,\"message\":" + q(msg == null ? "Something went wrong" : msg) + "}"; }
}
