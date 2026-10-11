# SmartPark - Project Submission Form Answers

### 1. Project Abstract Title
**SmartPark – Smart Parking Management System**  
*Tagline: Park Smarter | Save Time | Move Better*

---

### 2. About Project (150–200 words)
SmartPark is an end-to-end full-stack web application designed to eliminate urban parking congestion by seamlessly connecting Drivers, Parking Operators, and Facility Administrators within a unified platform. Vehicle owners can effortlessly register vehicles, explore real-time parking slot vacancies across multiple zones, reserve dedicated bays in advance, and access instant digital QR passes with automated grace-period billing. On-site parking operators utilize an interactive visual occupancy grid and streamlined gate check-in/check-out tools with instant cash recording. Administrators gain total governance through dynamic tariff management, staff access controls, immutable security audit logs, and interactive Chart.js analytics tracking revenue and occupancy trends.

Developed using Java 17 and Spring Boot 3 with Spring Security 6, Hibernate ORM, and responsive Thymeleaf with Bootstrap 5, SmartPark guarantees high reliability through pessimistic database row locking and optimistic locking to prevent double bookings. An automated Spring background scheduler sweeps expired reservations every minute. The system is containerized with multi-stage Docker and fully prepared for cloud deployment on Render, Railway, Vercel, and GitHub Actions CI.

---

### 3. Frontend Repository Link
`https://github.com/YOUR_GITHUB_USERNAME/smartpark`  
*(Note: Monorepo architecture — frontend is integrated server-side with Thymeleaf 3, Bootstrap 5, HTML5/CSS3, and vanilla JS under `src/main/resources/templates` and `src/main/resources/static`)*

---

### 4. Backend Repository Link
`https://github.com/YOUR_GITHUB_USERNAME/smartpark`  
*(Note: Monorepo architecture — Spring Boot 3 REST/MVC backend, JPA persistence, and services reside under `src/main/java/com/smartpark`)*

---

### 5. Deployment Link
- **Live Cloud Web Application (Render / Railway):** `https://smartpark-system.onrender.com`  
- **Vercel Proxy / Static Edge:** `https://smartpark.vercel.app`  

> **Architecture Monorepo Note:** SmartPark is engineered as a production-grade single monorepo. Both the backend business logic and the responsive, server-rendered frontend UI live synchronously within the same repository to maintain atomic versioning, zero CORS overhead, and streamlined continuous deployment.
