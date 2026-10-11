# SmartPark - System Architecture

```mermaid
graph TD
    subgraph ClientLayer["🖥️ Frontend & Presentation Layer"]
        UI["Bootstrap 5 + HTML5 + CSS3"]
        Thymeleaf["Thymeleaf Template Engine"]
        Charts["Chart.js Analytics Engine"]
        QR["ZXing Base64 Digital Passes"]
    end

    subgraph SecurityLayer["🛡️ Security & Authentication Layer"]
        SecFilter["Spring Security 6 Filter Chain"]
        AuthProv["DaoAuthenticationProvider"]
        BCrypt["BCrypt Password Encoder"]
        RoleAccess["Role-Based Access Control (Driver / Operator / Admin)"]
    end

    subgraph AppLayer["⚙️ Spring Boot Application Core"]
        Controllers["Spring MVC Controllers"]
        subgraph Services["Domain Service Interfaces & Implementations"]
            BookingSvc["BookingService (Locking & Concurrency)"]
            PricingSvc["PricingService (Dynamic Tariff Calculator)"]
            VehicleSvc["VehicleService (Garage Management)"]
            ZoneSlotSvc["ParkingZone & Slot Service"]
            PaymentSvc["PaymentService (Sandbox Gateway)"]
            NotifSvc["NotificationService"]
            AuditSvc["AuditLogService"]
        end
        Scheduler["@Scheduled Sweeper (1-Min Expiration Sweep)"]
    end

    subgraph DataLayer["💾 Persistence Layer"]
        JPA["Spring Data JPA & Hibernate ORM"]
        Repo["JPA Repositories with Optimistic & Pessimistic Locks"]
    end

    subgraph StorageLayer["🗄️ Database Storage"]
        H2["H2 In-Memory Database (Profile: local & test)"]
        MySQL["MySQL 8.0 Engine (Profile: prod)"]
    end

    UI --> SecFilter
    Thymeleaf --> UI
    Charts --> UI
    QR --> UI
    SecFilter --> Controllers
    Controllers --> Services
    Scheduler --> BookingSvc
    Services --> Repo
    Repo --> JPA
    JPA --> H2
    JPA --> MySQL
```

## Architectural Highlights
1. **Separation of Concerns:** Strict layered design from controller -> service (interfaces + implementations) -> repository -> entities. Views consume clean DTOs and view models.
2. **Double Booking Prevention:** Combines pessimistic database row locking (`findByIdWithPessimisticLock`) with JPA `@Version` optimistic concurrency control and an overlapping temporal range query.
3. **Automated Expiry Sweeper:** Background cron daemon sweeping every 60 seconds to release un-checked-in slots past the 15-minute grace window.
4. **Dynamic Tariff Engine:** Evaluates duration, vehicle type, and zone overrides with a 15-minute free grace window and ceiling rounding.
