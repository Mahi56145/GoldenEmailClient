# 📨 Golden Email Client — JavaFX Desktop Application

**Internship:** CodeClause – Email Client Software in Java (Golden Level)  
**Author:** Mahipal Mali  
**Project Duration:** 01 December 2025 – 31 December 2025  
**Supported Mail Provider:** Gmail (via App Password)  
**Tech Stack:** Java 21 • JavaFX • Maven • Jakarta Mail

---

## 📌 Project Description

Golden Email Client is a professional-grade desktop email software that enables users to:

- Login securely using Gmail App Passwords  
- Send rich formatted emails (HTML)
- Receive and read emails from inbox/sent folders
- Download attachments
- Compose messages with full text styling
- Switch between Light and Dark UI themes

---

## ✨ Features

- 🔒 **Secure Gmail authentication (App Password)**
- 📬 **View inbox and sent mail**
- 📝 **Rich HTML email composer**
- 📎 **Attachment upload & download**
- 🌙☀️ **Theme toggle (Dark / Light Mode)**
- 🚀 **Responsive, modern UI with JavaFX**
- 📁 **Clean CSS-managed styling**
- ✔️ **No UI freeze — uses background threading**

---

## 📁 Project Structure

src/
└── main/
├── java/
│ ├── AppLauncher.java
│ ├── EmailGUI.java
│ ├── EmailReceiver.java
│ └── EmailSender.java
└── resources/
├── dark.css
├── light.css
└── logo.png

pom.xml
README.md

---

## 🖥 Requirements

- Java **JDK 21+**  
- Maven **(installed & configured)**
- Internet connection  
- Gmail account with App Password

---

## 🔑 Gmail Login Instructions (Important)

You **cannot** use your normal Gmail login password.  
You must generate an **App Password**.

Steps:

1. Go to **Google Account → Security**
2. Enable **2-Step Verification**
3. Search "App Passwords" in the security menu
4. Generate a new password (name: *Golden Email Client*)
5. Use that **16-character password** in the login screen

---

## 🏗 Maven Build Instructions

Run in project root:

```bash
mvn clean package
```

This will generate the JAR at:

target/GoldenEmailClient-1.0.0.jar
(Version may vary based on pom.xml)

▶️ Run Application
```bash
java -jar target/GoldenEmailClient-1.0.0.jar
```

## OUTPUT
<img width="600" height="350" alt="image" src="https://github.com/user-attachments/assets/4a31a5c5-0912-41bb-b5e4-b884316e5d8d" /><img width="600" height="350" alt="image" src="https://github.com/user-attachments/assets/cd95f009-05c0-4beb-bde1-c10ab9023ab9" />
<img width="600" height="350" alt="image" src="https://github.com/user-attachments/assets/c2032d71-27dc-47f8-8e87-17de46f5cfac" /><img width="600" height="350" alt="image" src="https://github.com/user-attachments/assets/426f8cc6-dc18-485d-8472-a572ef974076" />




⚠️ Limitations

Currently supports Gmail only

Requires Gmail App Password (security requirement)

📚 Learnings From Project

Advanced Java & JavaFX development

Email protocols (IMAP/SMTP)

UI/UX theme management

File attachment handling

Working with Maven project structure

📌 Author

Mahipal Mali
Java Developer Intern — CodeClause
