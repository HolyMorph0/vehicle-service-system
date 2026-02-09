# Vehicle Service System

This project is a **Vehicle Service Tracking System** developed using **Java** as part of a university course project.  
The application aims to manage vehicles, drivers, maintenance records, and assignments in a structured way.

## Technologies Used
- Java
- Maven
- MySQL
- JDBC

## Project Structure
The project follows a layered architecture:
- `model` – Entity classes
- `dao` – Data Access Object (DAO) layer
- `db` – Database connection management
- `test` – CRUD and functional test classes

## Database
The application uses a **MySQL** database.  
SQL scripts for database schema and operations are provided in the repository.

Before running the project:
1. Create a MySQL database
2. Execute the provided SQL scripts
3. Update database connection settings in `DBConnection.java`

## Notes
This project was developed for educational purposes to improve skills in:
- Object-Oriented Programming (OOP)
- Database integration
- Layered application design
