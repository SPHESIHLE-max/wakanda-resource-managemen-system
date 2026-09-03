# Wakanda Resource Management System
Java Swing + OOP Project | NWU Vaal - CMPG 211 (2025)

A role-based desktop application for managing Wakanda's resources (e.g., Electricity) - tracks inventory, user access, and system activity.

## Features
- **Role-Based Login:** Administrator (full control) vs NormalUser (view/request)
- **Resource Management:** Add, update, delete resources (ElectricityResource)
- **Authentication:** Secure login with AuthenticationService & PasswordUtil
- **Audit & Reports:** SystemLog for tracking actions and ReportGenerator for reports
- **GUI:** Built with Java Swing in MainGUI.java

## Project Structure
- `src/` - Java source code (.java)
- `images/` - UI icons and backgrounds used in GUI

## Tech Stack
Java, OOP (Inheritance, Interfaces), Swing, File Handling

## How to Run
1. Open folder in NetBeans / IntelliJ
2. Run `MainGUI.java`
3. Login as Admin to manage resources

## What I Learned
- Implementing OOP: Manageable interface, inheritance between roles
- Authentication system
- Separation of logic (ResourceManager) and UI (MainGUI)
- Implementing OOP principles: Manageable interface, inheritance between Administrator/NormalUser
- Building authentication system
- Separation of logic (ResourceManager) and UI (MainGUI)
