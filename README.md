# Library Management System (Java + MySQL + HTML/CSS/JS)

## Project Overview
A GitHub-ready academic project demonstrating Java OOP, JDBC, MySQL database handling, and a frontend UI.
The Java backend (JDBC + DAO pattern) talks to MySQL, and the HTML/CSS/JS frontend connects to it.

## Features
- Admin login and Member login
- Dashboard: Total Books, Total Members, Issued Books, Returned Books
- Books: Add, Update, Search, View
- Members: Add, Update, Search, View
- Issue book (availability check, saves record, reduces quantity)
- Return book (updates return date, increases quantity)
- Due date calculation (14 days) and late return checking

## Tech Stack
- Backend: Core Java (JDK 21), JDBC, OOP, Exception Handling, Collections Framework
- Database: MySQL (`library_management`)
- Frontend: HTML5, CSS3, JavaScript
- Tools: VS Code, Git + GitHub

## Database Schema
Database: `library_management`

| Table | Columns |
|---|---|
| admin | id, username, password |
| members | id, name, email, phone |
| books | id, title, author, quantity |
| issue_records | id, book_id, member_id, issue_date, due_date, return_date |

Full script: [`database/library.sql`](Library-Management-System/database/library.sql)

## Screenshots

### Login
![Login](Library-Management-System/screenshots/Login.png)

### Dashboard
![Dashboard](Library-Management-System/screenshots/Dashboard.png)

### Books
![Books](Library-Management-System/screenshots/Books.png)

### Issue Book
![Issue](Library-Management-System/screenshots/Issue.png)

## How to Run

### Requirements
- JDK 21
- MySQL Server (running)
- An internet connection for the first run (the MySQL JDBC driver is downloaded automatically)

### Steps
1. **Create the database.** Run `database/library.sql` in MySQL (safe to run more than once):
   - MySQL Workbench: open the file and click the lightning button, or
   - Terminal: `mysql -u root -p < database/library.sql`
2. **Start the backend.**
   - Windows: double-click `run.bat`
   - Linux / macOS: `./run.sh`

   On the first run the script downloads the MySQL JDBC driver into `backend/lib/`. It then asks for your MySQL root password, compiles the code and starts the server at `http://localhost:8080/api/`. Keep this window open while using the app.
3. **Open the frontend.** Open `frontend/index.html` in your browser.

### Login
- Admin: username `admin`, password `admin123`
- Member: the email and phone number saved when the admin added the member. Add a member from the Members page first.

### Optional: skip the password prompt
Set the `DB_PASSWORD` environment variable once, and the script will not ask for the password again.
- Windows: `setx DB_PASSWORD your_password` (then reopen the terminal)
- Linux / macOS: `export DB_PASSWORD=your_password`

### Manual run (without the scripts)
Download `mysql-connector-j-8.4.0.jar` from Maven Central and place it in `backend/lib/`. Then, from the `backend` folder:

Windows:

mkdir out
javac -cp “lib/” -d out src\Main.java src\model*.java src\dao*.java src\database*.java src\exception*.java
set DB_PASSWORD=your_password
java -cp “out;lib/” Main

Linux / macOS:

mkdir -p out
javac -cp “lib/” -d out $(find src -name “.java”)
DB_PASSWORD=your_password java -cp “out:lib/*” Main
