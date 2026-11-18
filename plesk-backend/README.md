# LoyPOS Plesk MySQL Backend (`novelsapps.com.et`)

Server facts (from the Plesk panel, November 2026):

| Item | Value |
| --- | --- |
| Server IP | `213.55.96.154` |
| Domain | `novelsapps.com.et` (HTTPS, Let's Encrypt, HTTP→HTTPS 301) |
| System / FTP user | `novelsap` |
| SSH access | **Forbidden** — everything is done via Plesk File Manager / FTP / browser |
| Document root | `/httpdocs` |
| Scripting | PHP via FastCGI (CGI/SSI enabled) |
| Database | **Must be created first** — panel shows "Database is not created" |

## One-click setup (recommended)

1. **Create the database** — Plesk → *Databases* → *Add database*:
   - Database name: e.g. `loypos` (Plesk prefixes it → `novelsap_loypos`)
   - Related user + password: use the strong password you generated (rotate the one pasted in chat).
   - Note the exact **database name**, **user** and **password** shown by Plesk.

2. **Upload the API** — copy everything from `plesk-backend/api/v1/`
   (including `install.php`, `index.php`, `.htaccess`, `config.example.php`)
   into `httpdocs/api/v1/` using Plesk **File Manager** (or FTP as user `novelsap`).
   There is also a prebuilt `plesk-deploy-bundle.zip` to upload and extract in place.

3. **Run the installer** — open:
   ```
   https://novelsapps.com.et/api/v1/install.php?token=mequbbzjki
   ```
   - Enter the database name / user / password from step 1.
   - Leave **sync token** empty to auto-generate a strong one.
   - Press **Test connection & run migration**.
   - The migration is idempotent and non-destructive: it only runs
     `CREATE TABLE IF NOT EXISTS` and records applied steps in
     `schema_migrations`. **Existing rows are never modified or deleted** —
     the results page shows row counts before and after so you can verify.
   - Use **Report only** mode first if you just want to see what's in the DB.

4. **Copy the sync token** — the installer prints the generated token once
   (it is also stored in `httpdocs/api/v1/config.php`). Set the same value as
   `PLESK_SYNC_TOKEN` in the AI Studio Secrets panel (never commit `.env`).

5. **Verify & clean up** — with the token:
   ```
   curl -H "Authorization: Bearer <sync_token>" https://novelsapps.com.et/api/v1/health
   ```
   Expect `{"ok":true,...,"counts":{...}}`. Then **delete
   `httpdocs/api/v1/install.php`** via File Manager (the `install.lock`
   tombstone keeps browsers from re-running it in the meantime).

## Manual alternative (no installer)

- Import `plesk-backend/schema.sql` via Plesk → Databases → **phpMyAdmin** → Import.
- Copy `config.example.php` to `config.php` and fill in
  `db_name`, `db_user`, `db_pass` and a random 48-char `sync_token`.

## Files

| File | Purpose |
| --- | --- |
| `api/v1/index.php` | The REST sync API (`/health`, `/pull`, `/push`), Bearer-token auth |
| `api/v1/install.php` | One-click installer & idempotent migrator (delete after setup) |
| `api/v1/config.example.php` | Template — the real `config.php` is created on the server only |
| `api/v1/.htaccess` | Routes requests to `index.php`, passes Authorization header |
| `api/v1/proxy.php` | Server-side proxy example (same array config as `index.php`) |
| `schema.sql` | Full schema, MySQL 8 / MariaDB 10.4+, `utf8mb4` |
