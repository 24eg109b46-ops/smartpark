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
`https://github.com/YOUR_GITHUB_USERNAME/smartpark/tree/main/frontend`  
*(Standalone Single-Page Web Client ready for Vercel deployment under `frontend/`)*

---

### 4. Backend Repository Link
`https://github.com/YOUR_GITHUB_USERNAME/smartpark/tree/main/backend`  
*(Java 17 + Spring Boot 3.3.4 REST API & MVC backend with Maven build under `backend/`)*

---

### 5. Deployment Link
- **Live Backend REST & MVC Service (Render):** `https://smartpark-backend.onrender.com`  
- **Live Frontend Web Client (Vercel):** `https://smartpark-client.vercel.app`  

> **Separated Monorepo Architecture:** SmartPark is organized into distinct `backend/` and `frontend/` directories within a unified GitHub repository. The backend is built and tested via Maven/Docker for one-click deployment on Render, while the frontend is configured with `vercel.json` for edge hosting on Vercel with seamless cross-origin REST communication.
