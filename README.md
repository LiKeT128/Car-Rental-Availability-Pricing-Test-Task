# Car Rental Availability & Pricing System

Toto je riešenie fullstack zadania pre systém overovania dostupnosti a cien prenájmu áut. Projekt pozostáva z **Java Spring Boot** backendu a **Next.js** (App Router) frontendu.

## 🎯 Ciele riešenia
- Robustné spracovanie "nečistých" dát z JSON súborov.
- Výpočet dostupnosti na základe prekrývajúcich sa intervalov.
- Klasifikácia konfliktov (HARD, SOFT, DATA).
- Moderné a responzívne používateľské rozhranie.

## 🚀 Spustenie projektu

### 1. Backend (Java Spring Boot)
Backend beží na porte **8080**.
```bash
cd backend
java -D"maven.multiModuleProjectDirectory=$PWD" -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
```

### 2. Frontend (Next.js)
Frontend beží na porte **3000**.
```bash
cd frontend
npm install
npm run dev
```

## 📁 Štruktúra projektu
- `backend/`: Zdrojový kód API v Jave.
- `frontend/`: UI aplikácia v Next.js.
- `data/`: Vstupné JSON dáta (cars.json, reservations.json).
- `DECISIONS.md`: Log dôležitých rozhodnutí a analýza dát.

## 🧪 Testovanie
Backend obsahuje unit testy pre overenie správnosti logiky prekrývania a výpočtu konfliktov.
Spustenie testov:
```bash
cd backend
java -D"maven.multiModuleProjectDirectory=$PWD" -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain test
```

---
## 🇺🇸 ENG Version

This is a fullstack assignment solution for a car rental availability and pricing verification system. The project consists of a **Java Spring Boot** backend and a **Next.js** (App Router) frontend.

## 🎯 Solution goals
- Robust processing of "dirty" data from JSON files.
- Availability calculation based on overlapping intervals.
- Conflict classification (HARD, SOFT, DATA).
- Modern and responsive user interface.

## 🚀 Project launch

### 1. Backend (Java Spring Boot)
The backend runs on port **8080**.
```bash
cd backend
java -D"maven.multiModuleProjectDirectory=$PWD" -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain spring-boot:run
```

### 2. Frontend (Next.js)
The frontend runs on port **3000**.
```bash
cd frontend
npm install
npm run dev
```

## 📁 Project structure
- `backend/`: API source code in Java.
- `frontend/`: UI application in Next.js.
- `data/`: Input JSON data (cars.json, reservations.json).
- `DECISIONS.md`: Log of important decisions and data analysis.

## 🧪 Testing
The backend contains unit tests.
Run the tests:
```bash
cd backend
java -D"maven.multiModuleProjectDirectory=$PWD" -cp ".mvn\wrapper\maven-wrapper.jar" org.apache.maven.wrapper.MavenWrapperMain test
```
