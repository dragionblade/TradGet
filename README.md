# Tradget: University Ride-Sharing Platform

Tradget is a specialized, peer-to-peer carpooling application engineered to streamline commuting within the university ecosystem. By connecting student drivers with passengers traveling along similar routes, Tradget aims to provide a transparent, cost-effective transportation solution while promoting a more sustainable campus environment.

## 🚧 Project Status: In Development (Frontend Only)

**Please Note: This project is currently in the active development phase. At present, only the frontend user interface and core navigation have been completed.** The current repository contains the Android UI architecture, layouts, and initial frontend API integrations. Backend services, database management, and the core routing algorithms are slated for upcoming releases.

---

## 🚀 Current Frontend Features

The following features have been implemented in the current Android build:

* **Core Navigation Architecture:** Implemented a robust `BottomNavigationView` handling seamless fragment transitions across the app.
* **Home / Ride Search Engine (`HomeFragment`):**
  * **User Roles:** Dynamic UI toggles for selecting between "Passenger" and "Rider" modes.
  * **Location Services:** Integration with `FusedLocationProviderClient` to automatically fetch and display the user's current location with necessary permission handling.
  * **Google Places Integration:** Fully functional autocomplete search dialog for selecting precise pickup and destination locations using the Google Places API.
* **Interactive Mapping (`MapActivity`):**
  * Initial integration with the Google Maps SDK, featuring automated camera positioning and marker rendering.
* **Chat Interface (`ChatFragment`):**
  * Prototyped peer-to-peer messaging UI with simulated asynchronous reply handling.
* **User Profile & History (`ProfileFragment`, `HistoryFragment`):**
  * UI layouts established for ride history sorting (Completed, Cancelled, Scheduled) and user settings, currently utilizing mock interactive toast events.

## 🛠️ Tech Stack (Frontend)

* **Language:** Java
* **Platform:** Android SDK
* **APIs & SDKs:**
  * Google Maps SDK for Android
  * Google Places API
  * Google Play Services Location (`FusedLocationProviderClient`)

## 🗺️ Roadmap / Upcoming Features

* **Backend Integration:** Setting up user authentication and database management.
* **Fuel-Cost-Splitting Algorithm:** The core logic to calculate and distribute travel expenses equitably among all vehicle occupants based on route distance and fuel prices.
* **Real-Time Ride Matching:** Backend logic to connect riders and drivers with overlapping routes.
* **Live Chat:** Upgrading the mocked chat interface to a real-time database (e.g., Firebase) for live peer-to-peer communication.

## ⚙️ Setup & Installation

1. Clone this repository: `git clone https://github.com/your-username/tradget.git`
2. Open the project in **Android Studio**.
3. **API Key Configuration:**
   * You will need a valid Google Maps & Google Places API Key.
   * Add your API key to your `local.properties` file or secure strings resource. *(Ensure API keys are never pushed to the public repository).*
4. Sync the project with Gradle files and run on an emulator or physical device.
