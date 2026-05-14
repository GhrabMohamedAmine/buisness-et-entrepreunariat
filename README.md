# Nexum JavaFX – Esprit PIDEV 3A3 (2025–2026)

Nexum JavaFX is a desktop version of the Nexum project built with **JavaFX** and **FXML**.  
It focuses on métier modules, desktop user experience, AI-assisted workflows using **LM Studio**, and external services deployed through **Docker**.

---

## Table of Contents

- [Overview](#overview)
- [Main Features](#main-features)
- [Tech Stack](#tech-stack)
- [Repository Structure](#repository-structure)
- [Architecture](#architecture)
- [Prerequisites](#prerequisites)
- [Installation & Setup](#installation--setup)
- [Environment Configuration](#environment-configuration)
- [Database Setup](#database-setup)
- [Run the Project](#run-the-project)
- [LM Studio Integration](#lm-studio-integration)
- [Docker External APIs](#docker-external-apis)
- [Métier Modules](#métier-modules)
- [Common Commands](#common-commands)
- [Security Notes](#security-notes)
- [Contributing](#contributing)
- [License](#license)

---

## Overview

Nexum JavaFX is a desktop application designed to help teams manage business workflows through a modular interface.

The application combines:

- JavaFX desktop screens
- FXML-based UI structure
- Controller-based business logic
- Database-backed métier modules
- AI features powered by LM Studio
- Dockerized external APIs and support services

The project is intended for academic, educational, and experimental use within the Esprit PIDEV context.

---

## Main Features

### 1. Desktop User Interface

- JavaFX-based desktop application
- FXML views for clean UI separation
- Controller classes for screen logic
- Scene navigation between modules
- TableView, forms, dialogs, and dashboard-style screens

### 2. Métier Management

- CRUD operations for core métier entities
- Form validation
- Search and filtering
- Entity relationships
- Business rules handled in service layers

### 3. AI Assistance with LM Studio

- Local AI assistance using LM Studio
- Prompt-based analysis
- AI-generated explanations and recommendations
- Support for local LLM experimentation
- Private AI workflow without depending on a cloud AI provider

### 4. External APIs with Docker

- External services can be started through Docker
- Local API services can be connected to the JavaFX app
- Useful for AI, authentication, media, messaging, or helper services
- Keeps external dependencies isolated from the desktop application

### 5. Database Integration

- Database-backed storage for application data
- DAO, service, or repository layer depending on implementation
- SQL-based persistence
- Local development database support

---

## Tech Stack

### Desktop Application

- Java
- JavaFX
- FXML
- Scene Builder
- CSS for JavaFX styling

### AI

- LM Studio
- Local LLM server
- HTTP API calls from Java
- JSON request and response handling

### External Services

- Docker
- Docker Compose
- Local API containers
- Optional helper services depending on project needs

### Database

- MySQL or compatible SQL database
- JDBC
- SQL queries
- DAO / Service architecture

---

## Repository Structure

```text
.
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── tn/esprit/nexum/
│   │   │       ├── controllers/
│   │   │       ├── entities/
│   │   │       ├── services/
│   │   │       ├── utils/
│   │   │       └── Main.java
│   │   └── resources/
│   │       ├── fxml/
│   │       ├── css/
│   │       ├── images/
│   │       └── config/
├── docker/
│   ├── compose.yaml
│   └── services/
├── database/
│   └── schema.sql
├── pom.xml
└── README.md
```

> The exact structure may differ depending on the final project organization.

---

## Architecture

Nexum JavaFX follows a layered desktop architecture.

### JavaFX Client

The JavaFX application is responsible for:

- Displaying screens
- Handling user interactions
- Loading FXML views
- Calling service classes
- Showing validation messages and results

### FXML Views

FXML files define the UI layout for each screen.

They are usually stored under:

```text
src/main/resources/fxml/
```

Each FXML file is linked to a Java controller.

### Controllers

Controllers handle UI logic such as:

- Button actions
- Form input reading
- TableView population
- Navigation between screens
- Calling service methods

### Services / Métier Layer

The service layer contains business logic.

It handles:

- Validation
- Data processing
- Communication between controllers and database access
- AI or external API requests when needed

### Database Layer

The database layer handles persistence using JDBC or DAO classes.

It is responsible for:

- Creating connections
- Running SQL queries
- Mapping database rows to Java objects
- Saving, updating, deleting, and retrieving data

### External APIs

External APIs are separated from the JavaFX app and can run through Docker.

Examples:

- AI service
- Media service
- Authentication helper
- Notification service
- Other local APIs required by métier features

---

## Prerequisites

Before running the project, install:

- Java JDK 17 or later
- JavaFX SDK compatible with your JDK
- Maven
- Scene Builder
- MySQL or compatible SQL database
- Docker Desktop
- LM Studio

Recommended tools:

- IntelliJ IDEA
- Git
- Postman or Insomnia for API testing

---

## Installation & Setup

### 1. Clone the Repository

```bash
git clone <repo-url>
cd <project-folder>
```

### 2. Open the Project

Open the project with IntelliJ IDEA or your preferred Java IDE.

### 3. Install Maven Dependencies

```bash
mvn clean install
```

### 4. Configure JavaFX

If JavaFX is not bundled with your JDK, configure the JavaFX SDK path in your IDE.

Example VM options:

```bash
--module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml
```

On Windows, the path may look like:

```bash
--module-path "C:\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml
```

### 5. Configure the Database

Create the database manually or import the SQL schema:

```bash
mysql -u root -p < database/schema.sql
```

Update the database configuration in the project according to your local setup.

---

## Environment Configuration

Create or update your local configuration file if your project uses one.

Example configuration values:

```properties
DB_URL=jdbc:mysql://localhost:3306/nexum
DB_USER=root
DB_PASSWORD=

LM_STUDIO_URL=http://localhost:1234/v1/chat/completions

EXTERNAL_API_URL=http://localhost:8080
```

Never commit real passwords, tokens, or private API keys.

---

## Database Setup

The application usually needs a SQL database containing the project entities.

Common setup flow:

```bash
mysql -u root -p
CREATE DATABASE nexum;
USE nexum;
SOURCE database/schema.sql;
```

Make sure the database connection settings match your local environment.

---

## Run the Project

### Run with Maven

```bash
mvn javafx:run
```

### Run from IDE

Run the main application class:

```text
Main.java
```

If needed, add the JavaFX VM options in the run configuration.

---

## LM Studio Integration

Nexum JavaFX can communicate with LM Studio using its local OpenAI-compatible API.

### 1. Start LM Studio

Open LM Studio and load a local model.

### 2. Enable the Local Server

Start the LM Studio local server.

Default local endpoint usually looks like:

```text
http://localhost:1234/v1/chat/completions
```

### 3. Connect JavaFX to LM Studio

The JavaFX app sends HTTP requests to LM Studio and receives AI-generated responses.

Typical use cases:

- Generate explanations
- Analyze métier data
- Suggest decisions
- Summarize information
- Help users understand project or finance data

### Example Request Body

```json
{
  "model": "local-model",
  "messages": [
    {
      "role": "system",
      "content": "You are an assistant integrated into the Nexum desktop application."
    },
    {
      "role": "user",
      "content": "Analyze this project status and suggest improvements."
    }
  ],
  "temperature": 0.7
}
```

---

## Docker External APIs

External APIs can be started using Docker Compose.

Example:

```bash
cd docker
docker compose up -d
```

Possible Dockerized services:

- AI helper API
- Authentication service
- File or media service
- Notification service
- Local testing service
- Database service

Example `compose.yaml` structure:

```yaml
services:
  external-api:
    image: external-api-image
    container_name: nexum-external-api
    ports:
      - "8080:8080"
    environment:
      - APP_ENV=dev
```

Check container status:

```bash
docker ps
```

Stop services:

```bash
docker compose down
```

---

## Métier Modules

The project can include several métier modules depending on the final implementation.

Examples:

### User Management

- Add, update, delete, and list users
- Authentication-related logic
- Role-based access logic

### Project Management

- Create and manage projects
- Assign users to projects
- Track project status and progress

### Task Management

- Create and assign tasks
- Track task status
- Manage deadlines and priorities

### Finance Management

- Manage budgets
- Track transactions
- Analyze expenses
- Generate AI-supported financial insights

### Communication

- Messaging or notification features
- Integration with external services when needed

### AI Métier Assistance

- AI-generated recommendations
- Local analysis with LM Studio
- Smart summaries for project, task, or finance data

---

## Common Commands

```bash
# Build the project
mvn clean install

# Run the JavaFX app
mvn javafx:run

# Run tests
mvn test

# Start Docker services
docker compose up -d

# Stop Docker services
docker compose down

# Check running containers
docker ps
```

---

## Security Notes

- Do not commit database passwords.
- Do not commit private API keys.
- Keep local configuration files private.
- Validate all user input before database operations.
- Use prepared statements to avoid SQL injection.
- Do not expose local LM Studio or Docker APIs publicly without protection.
- Keep Docker services limited to local development unless properly secured.

---

## Contributing

1. Create a feature branch.
2. Keep commits clear and focused.
3. Follow the existing package and naming conventions.
4. Keep FXML, controller, service, and entity logic separated.
5. Test your module before committing.
6. Document important changes.

---

## License

This project belongs to the **CCG Team**.

Use and distribution are subject to the CCG Team policy.
