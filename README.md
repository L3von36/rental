# LoyPOS — Event Equipment Rental (Android & Desktop)

LoyPOS is an offline-first **Event Equipment Rental** POS and fleet management platform built with **Kotlin**, **Jetpack Compose**, and **Material 3**, supporting both **Android mobile/tablet** devices (`app/`) and **Windows, macOS, and Linux desktop** computers (`desktop/`).

## Features

- **Event Equipment Fleet (52 Ready Items / 8 Categories)**
  - Pre-seeded catalog covering Tents & Canopies, Chairs & Seating, Tables & Linens, Stage & Lighting, Sound & AV, Staging & Flooring, Power & HVAC, and Decor & Catering.
  - Daily rental rate pricing (`qty × daily rate × rental days`) with replacement unit cost tracking and available fleet quantity management.
- **Rent Desk & Multi-Day Bookings**
  - Adaptive mobile phone UI (bottom bar + cart sheet) and wide tablet / desktop split-pane workspace (`Rent Desk` + persistent live booking panel).
  - Rental duration selector (`1d`, `2d`, `3d`, `7d`, `14d` presets + stepper) with automatic pickup-to-due return date calculation.
  - Event name, venue/delivery location, refundable security deposit, whole-rental `%` or fixed currency discounts, held quotes, and Cash/Card checkout.
- **Dispatch, Returns & Overdue Tracking**
  - Dedicated **Rentals & Returns** tracking (`Out Now`, `Overdue`, `Returned`, `Refunded`).
  - **Mark Equipment Returned** restores rented fleet quantities back to available stock with double-restore guards.
  - 80mm thermal rental PDF receipts (Android) and plain-text / CSV receipt & ledger exports (Android + Desktop).
- **Delivery & Pickup Dispatch Board**
  - Plan **delivery** and **pickup** runs to event venues with driver/vehicle assignment, on-site contact, load/packing list, and stop notes (gate codes, dock hours).
  - Stops auto-generate from rentals checked out with a venue (delivery at rental start, pickup after rental end), or can be scheduled manually from any active order.
  - Run lifecycle tracking (`Scheduled` → `En Route` → `Done` / `Failed`) with today-run and overdue sidebar badges on both platforms.
- **Back Office & CRM**
  - Client & Event Organizer directory, cash-drawer shifts (Android), daily revenue charts, multi-branch filtering, and optional Plesk MySQL REST cloud sync.
- **Mobile <-> Desktop Portability**
  - Identical `loypos-export` v1 JSON backup schema lets you export rental data from Android and import into Desktop (or vice versa).

## Local Build & Run

### Android (Mobile & Tablet)

```bash
gradle assembleDebug
```

### Desktop (Windows / macOS / Linux — JVM 21)

Run the standalone Compose Multiplatform Desktop app directly:

```bash
gradle -p desktop run
```

Run desktop unit tests or package native installers / runnable JAR:

```bash
gradle -p desktop test
gradle -p desktop packageDistributionForCurrentOS packageUberJarForCurrentOS
```

## GitHub Actions & Multi-Platform Releases

The repository workflow (`.github/workflows/release.yml`) automatically builds and attaches installers for all platforms:

- **Android (`build-android`)**: `loypos-debug.apk`, `loypos-release.apk`
- **Desktop (`build-desktop` matrix)**:
  - Linux (`ubuntu-latest`): `loypos-rental-linux.deb` + `loypos-rental-linux.jar`
  - Windows (`windows-latest`): `loypos-rental-windows.msi` + `loypos-rental-windows.jar`
  - macOS (`macos-latest`): `loypos-rental-macos.dmg` + `loypos-rental-macos.jar`
- **Triggers**: Pushes to `main` / `master` (auto-tags `v1.0.<run_number>`), version tag pushes (`v*`), or manual `workflow_dispatch`.

## License

MIT - see [LICENSE](LICENSE).
