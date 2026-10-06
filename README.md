# 🎓 Student Management System & Web Portal

A complete **Student Management System & Web Portal** built with **Java**, **Core JDBC**, and **MySQL**.  
Runs as a local web application on `http://localhost:8080` with a modern, responsive web dashboard and REST API, backed directly by MySQL.

---

## 🛠 Requirements

Before running this project, make sure you have:

| Tool               | Version       | Download Link |
|--------------------|---------------|---------------|
| **JDK**            | 17 or higher  | [Oracle JDK](https://www.oracle.com/java/technologies/downloads/) or [OpenJDK](https://adoptium.net/) |
| **MySQL Server**   | 8.0+          | [MySQL Downloads](https://dev.mysql.com/downloads/mysql/) |
| **MySQL Workbench**| Latest        | [MySQL Workbench](https://dev.mysql.com/downloads/workbench/) |
| **Apache Maven**   | 3.8+          | [Maven Downloads](https://maven.apache.org/download.cgi) |

### Verify Installation

Open a terminal/command prompt and run:

```bash
java --version
mvn --version
mysql --version
```

All three commands should return version numbers.

---

## 🗄 Database Setup (Step-by-Step)

### Step 1: Start MySQL Server

Make sure your MySQL server is running. On Windows, you can check this in **Services** (`services.msc`) → look for **MySQL80**.

### Step 2: Open MySQL Workbench

1. Open **MySQL Workbench**
2. Click on your **Local instance** connection (usually `root@localhost:3306`)
3. Enter your MySQL root password

### Step 3: Run the SQL Script

1. In MySQL Workbench, go to **File → Open SQL Script**
2. Navigate to: `StudentManagementSystem/database/student_db.sql`
3. Click the **⚡ Execute** button (lightning bolt icon)
4. You should see:
   - Database `student_db` created
   - Table `students` created
   - 5 sample students inserted

### Step 4: Verify

Run this in MySQL Workbench:

```sql
USE student_db;
SELECT * FROM students;
```

You should see 5 rows.

---

## 🔑 Configure Database Credentials

The application needs your MySQL username and password to connect.

### Option A: Edit Default Values (Simplest)

Open the file:

```
src/main/java/com/studentmanagement/config/DatabaseConnection.java
```

Find these lines and change them to match your MySQL credentials:

```java
private static final String DEFAULT_URL  = "jdbc:mysql://localhost:3306/student_db";
private static final String DEFAULT_USER = "root";       // ← Change if needed
private static final String DEFAULT_PASS = "root";       // ← Change to YOUR MySQL password
```

### Option B: Use Environment Variables (Advanced)

Set these environment variables before running:

**Windows (Command Prompt):**
```cmd
set DB_URL=jdbc:mysql://localhost:3306/student_db
set DB_USER=root
set DB_PASSWORD=your_password_here
```

**Windows (PowerShell):**
```powershell
$env:DB_URL = "jdbc:mysql://localhost:3306/student_db"
$env:DB_USER = "root"
$env:DB_PASSWORD = "your_password_here"
```

**Linux/Mac:**
```bash
export DB_URL=jdbc:mysql://localhost:3306/student_db
export DB_USER=root
export DB_PASSWORD=your_password_here
```

---

## 🚀 Build and Run

### Using Maven (Command Line)

```bash
# Navigate to the project directory
cd StudentManagementSystem

# Step 1: Clean and compile
mvn clean compile

# Step 2: Run the application
mvn exec:java
```

### Using an IDE (IntelliJ IDEA / Eclipse / VS Code)

1. **Import** the project as a **Maven project**
2. Let the IDE download dependencies (MySQL Connector/J)
3. Open `src/main/java/com/studentmanagement/Main.java`
4. Right-click → **Run 'Main.main()'**

---

## 📁 Project Structure

```
StudentManagementSystem/
│
├── pom.xml                          ← Maven build file (dependencies)
├── README.md                        ← This file
├── database/
│   └── student_db.sql               ← SQL script to create database & table
│
└── src/
    └── main/
        └── java/
            └── com/
                └── studentmanagement/
                    ├── Main.java                ← Entry point (menu)
                    │
                    ├── config/
                    │   └── DatabaseConnection.java   ← JDBC connection manager
                    │
                    ├── model/
                    │   └── Student.java              ← Student data model (POJO)
                    │
                    ├── dao/
                    │   └── StudentDAO.java            ← Database operations (CRUD)
                    │
                    ├── service/
                    │   └── StudentService.java        ← Business logic & validation
                    │
                    └── util/
                        └── InputValidator.java        ← Input validation utilities
```

### Why This Structure?

| Layer              | File                    | Responsibility                         |
|--------------------|-------------------------|----------------------------------------|
| **UI Layer**       | `Main.java`             | Menu display, user choice routing      |
| **Service Layer**  | `StudentService.java`   | Input collection, validation, display  |
| **DAO Layer**      | `StudentDAO.java`       | SQL queries, JDBC operations           |
| **Model**          | `Student.java`          | Data representation (one table row)    |
| **Config**         | `DatabaseConnection.java` | Database connection management       |
| **Utility**        | `InputValidator.java`   | Input validation rules                 |

---

## 🎯 Features

| Feature               | Description                                        |
|-----------------------|----------------------------------------------------|
| ➕ Add Student         | Add a new student with full validation             |
| 📋 View All Students   | Display all students in a formatted table          |
| 🔍 Search Students     | Search by Name, Email, Course, or Department       |
| 👤 View by ID          | View detailed information for a specific student   |
| ✏️ Update Student      | Update individual fields or all fields at once     |
| 🗑️ Delete Student      | Delete with confirmation prompt                    |
| ✅ Input Validation    | Name, email, phone, age, semester validation       |
| 🔒 Duplicate Detection | Prevents duplicate email addresses                 |
| 🛡️ Error Handling      | Graceful handling of all errors                    |

---

## 🧪 Testing Checklist

After running the application, test these scenarios:

| #  | Test Case                     | Expected Result                        |
|----|-------------------------------|----------------------------------------|
| 1  | Add a new student             | Student saved, success message         |
| 2  | View all students             | Table with all students displayed      |
| 3  | Search by name                | Matching students shown                |
| 4  | View student by ID            | Detailed student info displayed        |
| 5  | Update a student's name       | Name updated, success message          |
| 6  | Delete a student              | Confirmation asked, then deleted       |
| 7  | Add duplicate email           | Rejected with error message            |
| 8  | Enter invalid age (e.g., 5)   | Re-prompted for valid age              |
| 9  | Enter empty name              | Re-prompted for valid name             |
| 10 | Stop MySQL, then run app      | Connection error with helpful message  |

---

## 📝 JDBC Concepts Demonstrated

This project demonstrates the following core JDBC concepts:

| Concept              | Where Used                     | What It Does                           |
|----------------------|--------------------------------|----------------------------------------|
| JDBC Driver          | `pom.xml`                      | MySQL Connector/J JAR                  |
| `Connection`         | `DatabaseConnection.java`      | Active session with MySQL              |
| `DriverManager`      | `DatabaseConnection.java`      | Creates database connections           |
| `PreparedStatement`  | `StudentDAO.java`              | Safe parameterized SQL queries         |
| `ResultSet`          | `StudentDAO.java`              | Holds query results (rows)             |
| `executeQuery()`     | `StudentDAO.java`              | Runs SELECT queries                    |
| `executeUpdate()`    | `StudentDAO.java`              | Runs INSERT/UPDATE/DELETE              |
| `SQLException`       | All DAO methods                | Database error handling                |
| `try-with-resources` | All DAO methods                | Auto-closes DB resources               |
| CRUD Operations      | `StudentDAO.java`              | Create, Read, Update, Delete           |

---

## 🏗 Architecture

```
             USER
               │
               ▼
          Main.java          ← Displays menu, routes choices
               │
               ▼
       StudentService        ← Collects input, validates, formats output
               │
               ▼
         StudentDAO          ← Executes SQL using PreparedStatement
               │
               ▼
       JDBC Connection       ← DriverManager.getConnection()
               │
               ▼
            MySQL            ← Database server
               │
               ▼
          student_db         ← Database
               │
               ▼
           students          ← Table (id, name, email, ...)
```

---

## ❓ Troubleshooting

### "Cannot connect to MySQL database"
- Is MySQL running? Check Windows Services or run `mysqladmin ping`
- Did you run `student_db.sql`? The database must exist
- Is the password correct in `DatabaseConnection.java`?
- Is MySQL on port 3306? Check with `mysql -u root -p`

### "Access denied for user 'root'"
- Your MySQL password in `DatabaseConnection.java` doesn't match
- Edit `DEFAULT_PASS` to your actual MySQL root password

### "Unknown database 'student_db'"
- You haven't run the SQL script yet
- Open MySQL Workbench → run `database/student_db.sql`

### Maven build fails
- Run `mvn --version` to verify Maven is installed
- Make sure `JAVA_HOME` is set to your JDK directory
- Try: `mvn clean compile -U` (forces dependency download)

---

## 📜 License

This project is created for educational purposes. Feel free to use and modify.
