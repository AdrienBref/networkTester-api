# NetworkTester

NetworkTester is a backend application designed to monitor network devices, track their connectivity status, and manage alerting rules.  
It has been developed as a **portfolio project**, with a strong focus on backend architecture, clarity of design, and real-world use cases.

---

## 🎯 Project Purpose

The goal of this project is to demonstrate:

- Backend development with **Spring Boot**
- Clean architecture and separation of concerns
- REST API design
- Business logic encapsulation in service layers
- Integration with a lightweight frontend for visualization
- Real-time updates using WebSockets

This is **not a production-ready system**, but a realistic technical demo intended to showcase backend skills.

---

## 🧩 Main Features

- Register and manage network devices
- Check device connectivity status (ping-based logic)
- Store and update device runtime status
- Configure alert email settings
- Notify frontend clients of status changes via WebSocket
- RESTful API for full CRUD operations

---

## 🏗️ Architecture Overview

The project follows a classic layered architecture:

- **Controller**  
  Exposes REST endpoints and handles HTTP requests

- **Service**  
  Contains business logic and orchestration

- **Repository**  
  Handles persistence and database access

- **DTOs**  
  Defines API contracts and decouples external models from entities

- **Entities**  
  Represents the domain model

- **Exception Handling**  
  Centralized error handling with custom exceptions

---

## 🖥️ Frontend Integration

The backend can optionally serve a lightweight frontend (HTML / CSS / JavaScript) for demo purposes.

- The frontend consumes the REST API
- Uses WebSocket (STOMP / SockJS) for real-time device updates
- Displays device status, latency, and configuration options

The frontend is intentionally simple and framework-free to keep the focus on backend logic.

---

## 🚀 How to Run

### Requirements
- Java 17+
- Maven

### Run locally
```bash
mvn spring-boot:run
