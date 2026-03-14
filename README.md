# Online Voting Backend

Spring Boot backend for the Online Voting System frontend.

## Tech
- Spring Boot
- Java 25
- MySQL 8
- Maven

## Import Into STS
1. Open STS.
2. Choose `File -> Import -> Existing Maven Projects`.
3. Select the `online-voting-backend` folder.
4. Set the installed JDK to Java 25.

## MySQL Setup
1. Create or open a MySQL 8 connection.
2. Run `src/main/resources/schema.sql`.
3. Run `src/main/resources/data.sql`.
4. Update `src/main/resources/application.properties` with your MySQL password.

## Run
```bash
mvn clean install
mvn spring-boot:run
```

The API runs on `http://localhost:8080/api`.
