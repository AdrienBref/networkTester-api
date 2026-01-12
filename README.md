# ARGOS NetworkTester

ARGOS NetworkTester is a backend-focused monitoring application designed to track network device availability, connectivity status, and alerting rules in real time.  
It has been developed as a **portfolio project**, with a strong emphasis on clean backend architecture, clarity of design, and real-world system thinking.

---

## 🎯 Project Purpose

The goal of this project is to demonstrate:

- Backend development with **Spring Boot**
- Clean architecture and separation of concerns
- REST API design
- Business logic encapsulation in service layers
- Integration with a lightweight frontend for visualization
- Real-time updates using **WebSockets**

This is **not a production-ready system**, but a realistic technical demo intended to showcase backend and system-oriented skills.

---

## 🧩 Main Features

- Register and manage network devices
- Check device connectivity status (ping-based logic)
- Store and update device runtime status
- Configure alert email settings
- Notify frontend clients of status changes via WebSocket
- RESTful API supporting full CRUD operations

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
- Uses **WebSocket (STOMP / SockJS)** for real-time device updates
- Displays device status, latency, and configuration options

The frontend is intentionally simple and framework-free to keep the focus on backend logic.

---

## ⚙️ Configuration

To run the application locally, create an `application.properties` file in  
`src/main/resources/` based on the provided example:

```bash
cp application.example.properties application.properties
