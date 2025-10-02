# Bakery Notification Service

## Overview
Responsible for sending notifications (email, SMS, push, in-app), managing templates, campaigns, and analytics tracking.

## Features
- Multi-channel notification delivery
- Template management with dynamic variables
- Campaign & batch job management
- Delivery status tracking & analytics

## Dependencies
- Spring WebFlux
- Spring Data JPA
- Spring Security
- AWS SDK (SNS)
- Twilio SDK
- Thymeleaf
- Spring Boot Actuator

## Key Endpoints
- `/api/notifications/`
- `/api/notifications/templates`
- `/api/notifications/campaigns`
- `/api/notifications/statistics`

## Running
./gradlew bootRun

Runs on port 8083 by default.

## Documentation
Swagger UI: `http://localhost:8083/swagger-ui.html`

---