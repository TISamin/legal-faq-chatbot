# Legal FAQ Chatbot (Backend + Frontend)

This package contains a **ready-to-run** Spring Boot backend and a simple **HTML/CSS/JS** frontend.

## What you need installed
- Java JDK 21
- Maven
- MySQL Server (remember your root password)
- VS Code (recommended) + Live Server extension (optional)

---

## STEP 1 — Create the database
Open **MySQL Workbench** or **Command Prompt** and run:
```sql
CREATE DATABASE faqdb;
```
> You don’t need to create the `faq` table manually — Spring will do it.

---

## STEP 2 — Configure backend MySQL password
Open: `backend/src/main/resources/application.properties`  
Find this line and put your real password:
```
spring.datasource.password=YOUR_PASSWORD
```
If your MySQL user isn’t `root`, change `spring.datasource.username` too.

---

## STEP 3 — Run the backend
In a terminal:
```bash
cd backend
mvn spring-boot:run
```
You should see: `Tomcat started on port(s): 8080`.

### Test the API in your browser:
```
http://localhost:8080/api/faq?question=What is bail&language=EN
```
or the alias:
```
http://localhost:8080/api/faq/ask?question=What is bail&lang=en
```
You should get a plain text answer.

---

## STEP 4 — Open the frontend
Open `frontend/index.html` in your browser.  
(Or in VS Code, right-click → **Open with Live Server**.)

Type a question like **"What is bail?"**, language **English** → see the answer.
Switch language to **বাংলা** and ask **"জামিন কী?"**.

---

## How the pieces connect
- Frontend sends requests to: `http://localhost:8080/api/faq`
- Backend reads the `question` and `language` (or `lang`) parameters
- Backend searches the database and returns a text answer

---

## Common issues & fixes

### 1) 404 Not Found
- Make sure you used `/api/faq` or `/api/faq/ask` exactly.
- Backend must be running (`mvn spring-boot:run`).

### 2) Cannot connect to database
- Error like `Access denied for user` → check username/password in `application.properties`.
- Error like `Communications link failure` → ensure **MySQL service is running**.

### 3) Port 8080 already in use
- Change backend port by adding this line in `application.properties`:
  ```
  server.port=8081
  ```
- Update frontend `script.js` API_URL accordingly.

### 4) CORS blocked
- This project already sets `@CrossOrigin(origins="*")` in the controller.
- If still blocked, use Live Server (serves frontend via http://localhost:5500).

### 5) No results (always fallback)
- Make sure `data.sql` ran:
  - We set `spring.sql.init.mode=always` and `spring.jpa.defer-datasource-initialization=true`.
- Or insert your own FAQs into table `faq`.

---

## Where to add your own FAQs
Use MySQL Workbench:
```sql
USE faqdb;
INSERT INTO faq (question, answer, language) VALUES
('How long can police detain me?', 'You must be produced before a magistrate within 24 hours of arrest.', 'EN');
```
Repeat with `language = 'BN'` for Bangla versions.

---

## File map
```
backend/
  pom.xml
  src/main/java/com/example/faq/LegalFaqChatbotApplication.java
  src/main/java/com/example/faq/controller/FaqController.java
  src/main/java/com/example/faq/model/Faq.java
  src/main/java/com/example/faq/repository/FaqRepository.java
  src/main/java/com/example/faq/service/FaqService.java
  src/main/resources/application.properties
  src/main/resources/data.sql

frontend/
  index.html
  style.css
  script.js
```

Good luck with your demo! 🎯
