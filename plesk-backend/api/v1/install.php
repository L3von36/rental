<?php
/**
 * LoyPOS one-click installer & idempotent schema migrator.
 *
 * Upload to httpdocs/api/v1/install.php, then open in a browser:
 *   https://novelsapps.com.et/api/v1/install.php?token=mequbbzjki
 *
 * Safe to run on a database that already contains data:
 *   - tables are created with CREATE TABLE IF NOT EXISTS (no drops, no truncates)
 *   - applied steps are recorded in schema_migrations and never re-run
 *   - "Report only" mode shows current row counts without touching anything
 *
 * Delete this file (and install.lock stays as a tombstone) once the
 * /health endpoint answers OK.
 */

declare(strict_types=1);
error_reporting(E_ALL);
ini_set('display_errors', '0');

// ── Gate ────────────────────────────────────────────────────────────────────
const SETUP_TOKEN = 'mequbbzjki';
const LOCK_FILE   = __DIR__ . '/install.lock';

if (!isset($_GET['token']) || !hash_equals(SETUP_TOKEN, (string)$_GET['token'])) {
    http_response_code(403);
    exit('Forbidden: open this installer as install.php?token=' . SETUP_TOKEN);
}

header('Content-Type: text/html; charset=utf-8');

function e(?string $v): string { return htmlspecialchars((string)$v, ENT_QUOTES, 'UTF-8'); }

$locked = is_file(LOCK_FILE);
$reportOnly = isset($_POST['mode']) && $_POST['mode'] === 'report';

// Pre-fill from an existing config.php (shipped in the bundle or from a previous run)
$cfgExisting = null;
if (is_file(__DIR__ . '/config.php')) {
    try {
        $maybe = require __DIR__ . '/config.php';
        $cfgExisting = is_array($maybe) ? $maybe : null;
    } catch (Throwable $e) {
        $cfgExisting = null;
    }
}
$results = [];   // [section, label, status, detail]
$countsBefore = [];
$countsAfter = [];
$tables = [
    'branches', 'categories', 'products', 'customers', 'sales', 'sale_items',
    'stock_movements', 'shifts', 'cash_movements', 'shop_settings', 'schema_migrations',
];

// ── Migration steps (add new steps below; never edit an applied step) ───────
$MIGRATIONS = [
    '001_core_schema' => [
        "CREATE TABLE IF NOT EXISTS branches (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            address VARCHAR(500) NOT NULL DEFAULT '',
            phone VARCHAR(64) NOT NULL DEFAULT '',
            created_at BIGINT NOT NULL DEFAULT 0,
            archived TINYINT(1) NOT NULL DEFAULT 0,
            updated_at BIGINT NOT NULL DEFAULT 0,
            INDEX idx_branches_updated (updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS categories (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            position INT NOT NULL DEFAULT 0,
            updated_at BIGINT NOT NULL DEFAULT 0,
            INDEX idx_categories_updated (updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS products (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            category_id VARCHAR(64) NULL,
            name VARCHAR(255) NOT NULL,
            price_cents BIGINT NOT NULL DEFAULT 0,
            cost_cents BIGINT NOT NULL DEFAULT 0,
            sku VARCHAR(128) NOT NULL DEFAULT '',
            barcode VARCHAR(128) NOT NULL DEFAULT '',
            track_stock TINYINT(1) NOT NULL DEFAULT 0,
            stock DOUBLE NOT NULL DEFAULT 0,
            archived TINYINT(1) NOT NULL DEFAULT 0,
            branch_ids TEXT NOT NULL,
            image_path VARCHAR(512) NOT NULL DEFAULT '',
            updated_at BIGINT NOT NULL DEFAULT 0,
            INDEX idx_products_category (category_id),
            INDEX idx_products_sku (sku),
            INDEX idx_products_updated (updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS customers (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            name VARCHAR(255) NOT NULL,
            phone VARCHAR(64) NOT NULL DEFAULT '',
            email VARCHAR(255) NOT NULL DEFAULT '',
            note TEXT NOT NULL,
            points INT NOT NULL DEFAULT 0,
            created_at BIGINT NOT NULL DEFAULT 0,
            updated_at BIGINT NOT NULL DEFAULT 0,
            INDEX idx_customers_updated (updated_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS sales (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            number INT NOT NULL,
            created_at BIGINT NOT NULL,
            subtotal_cents BIGINT NOT NULL,
            discount_cents BIGINT NOT NULL DEFAULT 0,
            discount_type VARCHAR(32) NULL,
            discount_value DOUBLE NULL,
            tax_cents BIGINT NOT NULL DEFAULT 0,
            total_cents BIGINT NOT NULL,
            paid_cents BIGINT NOT NULL,
            change_cents BIGINT NOT NULL DEFAULT 0,
            payment_type VARCHAR(32) NOT NULL DEFAULT 'cash',
            customer_id VARCHAR(64) NULL,
            customer_name VARCHAR(255) NULL,
            status VARCHAR(32) NOT NULL DEFAULT 'completed',
            note TEXT NOT NULL,
            branch_id VARCHAR(64) NULL,
            cashier_id VARCHAR(64) NULL,
            cashier_name VARCHAR(255) NULL,
            updated_at BIGINT NOT NULL DEFAULT 0,
            INDEX idx_sales_created (created_at),
            INDEX idx_sales_updated (updated_at),
            INDEX idx_sales_branch (branch_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS sale_items (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            receipt_id VARCHAR(64) NOT NULL,
            item_id VARCHAR(64) NOT NULL,
            name VARCHAR(255) NOT NULL,
            price_cents BIGINT NOT NULL,
            qty DOUBLE NOT NULL,
            line_total_cents BIGINT NOT NULL,
            INDEX idx_sale_items_receipt (receipt_id),
            INDEX idx_sale_items_item (item_id)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS stock_movements (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            item_id VARCHAR(64) NOT NULL,
            qty DOUBLE NOT NULL,
            type VARCHAR(32) NOT NULL,
            created_at BIGINT NOT NULL,
            note TEXT NOT NULL,
            receipt_id VARCHAR(64) NULL,
            INDEX idx_stock_movements_item (item_id),
            INDEX idx_stock_movements_created (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS shifts (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            opened_at BIGINT NOT NULL,
            opening_float_cents BIGINT NOT NULL DEFAULT 0,
            branch_id VARCHAR(64) NULL,
            closed_at BIGINT NULL,
            counted_cents BIGINT NULL,
            expected_cents BIGINT NULL,
            INDEX idx_shifts_opened (opened_at),
            INDEX idx_shifts_closed (closed_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS cash_movements (
            id VARCHAR(64) NOT NULL PRIMARY KEY,
            shift_id VARCHAR(64) NOT NULL,
            type VARCHAR(16) NOT NULL,
            amount_cents BIGINT NOT NULL,
            note TEXT NOT NULL,
            created_at BIGINT NOT NULL,
            INDEX idx_cash_movements_shift (shift_id),
            INDEX idx_cash_movements_created (created_at)
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS shop_settings (
            setting_key VARCHAR(128) NOT NULL PRIMARY KEY,
            setting_value TEXT NOT NULL,
            updated_at BIGINT NOT NULL DEFAULT 0
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
        "CREATE TABLE IF NOT EXISTS schema_migrations (
            step_id VARCHAR(64) NOT NULL PRIMARY KEY,
            applied_at BIGINT NOT NULL
        ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
    ],
];

function count_rows(PDO $pdo, array $tables): array {
    $out = [];
    foreach ($tables as $t) {
        try {
            $out[$t] = (int)$pdo->query("SELECT COUNT(*) FROM `" . str_replace('`', '', $t) . "`")->fetchColumn();
        } catch (Throwable $e) {
            $out[$t] = null; // table missing
        }
    }
    return $out;
}

$done = false;
$generatedToken = '';

if ($_SERVER['REQUEST_METHOD'] === 'POST') {
    // Form input wins; empty fields fall back to saved config.php, then defaults
    $resolve = static function (string $field, string $fallback = '') use ($cfgExisting): string {
        $v = trim((string)($_POST[$field] ?? ''));
        if ($v !== '') {
            return $v;
        }
        return (string)($cfgExisting[$field] ?? $fallback);
    };
    $dbHost  = $resolve('db_host', 'localhost');
    $dbPort  = (int)($resolve('db_port', '3306') ?: '3306');
    $dbName  = $resolve('db_name');
    $dbUser  = $resolve('db_user');
    $dbPass  = trim((string)($_POST['db_pass'] ?? '')) !== ''
        ? (string)$_POST['db_pass']
        : (string)($cfgExisting['db_pass'] ?? '');
    $syncTok = $resolve('sync_token');
    $tokenKept = false;
    $overwriteCfg = isset($_POST['overwrite_config']);

    try {
        if ($dbName === '' || $dbUser === '' || $dbPass === '') {
            throw new RuntimeException('Database name, user and password are required (no saved config.php found to fall back on).');
        }

        // 1. Connect — when host is "localhost" PHP uses the unix socket, which can
        //    point at a different MySQL instance (or none) on multi-instance boxes.
        //    If auth (1045), missing socket (2002) or unknown DB (1049) fails there,
        //    retry over TCP via 127.0.0.1 before giving up.
        $pdo = null;
        $connectErr = null;
        $candidates = $dbHost === 'localhost' ? ['localhost', '127.0.0.1'] : [$dbHost];
        foreach ($candidates as $tryHost) {
            try {
                $pdo = new PDO(
                    "mysql:host={$tryHost};port={$dbPort};dbname={$dbName};charset=utf8mb4",
                    $dbUser,
                    $dbPass,
                    [PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION, PDO::ATTR_TIMEOUT => 10]
                );
                $dbHost = $tryHost;
                break;
            } catch (PDOException $pe) {
                $connectErr = $pe;
                $em = $pe->getMessage();
                $retryable = stripos($em, '[1045]') !== false
                    || stripos($em, '[2002]') !== false
                    || stripos($em, '[1049]') !== false;
                if (!$retryable || $tryHost === $candidates[count($candidates) - 1]) {
                    throw $pe;
                }
            }
        }
        if (!$pdo instanceof PDO) {
            throw $connectErr ?? new RuntimeException('Could not connect to MySQL.');
        }
        $version = $pdo->query('SELECT VERSION()')->fetchColumn();
        $results[] = ['Connect', "MySQL/MariaDB {$version} via {$dbHost}:{$dbPort}", 'ok', "database `{$dbName}`"];

        // 2. Snapshot existing data BEFORE touching anything
        $countsBefore = count_rows($pdo, $tables);
        $existing = 0;
        foreach ($countsBefore as $c) { $existing += (int)$c; }
        $results[] = ['Existing data', $existing > 0 ? "{$existing} rows already present — nothing will be deleted" : 'database is empty', 'ok', 'report-only would stop here'];

        if ($reportOnly) {
            $countsAfter = $countsBefore;
            $results[] = ['Mode', 'Report only — no changes written', 'ok', ''];
            $done = true;
        } elseif ($locked) {
            $results[] = ['Install lock', 'install.lock already exists — migration skipped', 'warn', 'delete install.lock via File Manager to re-run'];
            $countsAfter = $countsBefore;
            $done = true;
        } else {
            // 3. Write config.php
            $cfgPath = __DIR__ . '/config.php';
            if ($syncTok === '') {
                $syncTok = bin2hex(random_bytes(24));
                $generatedToken = $syncTok;
            } else {
                $tokenKept = true;
            }
            $cfg = [
                'db_host' => $dbHost,
                'db_port' => $dbPort,
                'db_name' => $dbName,
                'db_user' => $dbUser,
                'db_pass' => $dbPass,
                'sync_token' => $syncTok,
            ];
            $php = "<?php\n// Generated by install.php on " . gmdate('c') . "\n// NEVER commit this file to version control.\n\nreturn " . var_export($cfg, true) . ";\n";
            if (!is_file($cfgPath) || $overwriteCfg) {
                file_put_contents($cfgPath, $php, LOCK_EX);
                @chmod($cfgPath, 0640);
                // Make sure PHP-FPM/CGI picks up the new config.php even on hosts
                // with opcache.validate_timestamps=0
                if (function_exists('opcache_reset')) {
                    @opcache_reset();
                }
                $results[] = ['config.php', $overwriteCfg ? 'overwritten' : 'written', 'ok', 'sync_token: ' . ($generatedToken ? 'auto-generated (shown below)' : ($tokenKept ? 'kept from saved config.php' : 'kept from your input'))];
            } else {
                $results[] = ['config.php', 'already exists — left untouched', 'warn', 'tick "Overwrite config.php" to replace it'];
            }

            // 4. Run pending migrations (idempotent)
            $applied = [];
            try { $applied = $pdo->query('SELECT step_id FROM schema_migrations')->fetchAll(PDO::FETCH_COLUMN); } catch (Throwable $e) {}
            foreach ($MIGRATIONS as $id => $statements) {
                if (in_array($id, $applied, true)) {
                    $results[] = ["Step {$id}", 'already applied — skipped', 'ok', ''];
                    continue;
                }
                foreach ($statements as $sql) {
                    $pdo->exec($sql);
                }
                $pdo->prepare('INSERT INTO schema_migrations (step_id, applied_at) VALUES (?, ?)')
                    ->execute([$id, (int)round(microtime(true) * 1000)]);
                $results[] = ["Step {$id}", count($statements) . ' statements executed', 'ok', 'CREATE TABLE IF NOT EXISTS — existing tables untouched'];
            }

            // 5. Snapshot after + lock
            $countsAfter = count_rows($pdo, $tables);
            file_put_contents(LOCK_FILE, (string)time());
            $results[] = ['Finish', 'install.lock written — migration complete', 'ok', ''];
            $done = true;
        }
    } catch (Throwable $ex) {
        $msg = $ex->getMessage();
        if (stripos($msg, '1049') !== false || stripos($msg, 'Unknown database') !== false) {
            $msg .= ' → create the database first: Plesk → Databases → Add database';
        }
        if (stripos($msg, '1045') !== false || stripos($msg, 'Access denied') !== false) {
            $msg .= ' → Fix steps: 1) Plesk → Databases → open your DB and check the EXACT database name and user name shown there (Plesk often prefixes them, e.g. novelsap_loypos / novelsap_novel) — type exactly those into the form. 2) Click the DB user → Change password, set it again carefully, type it into the form and tick "Overwrite config.php". 3) Try Database host 10.180.50.142 (the DB server address shown in Plesk) instead of localhost.';
        }
        $results[] = ['Error', $msg, 'err', 'nothing was changed if this happened before migration steps'];
        $done = true;
    }
}
?>
<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="utf-8">
<meta name="viewport" content="width=device-width, initial-scale=1">
<meta name="robots" content="noindex,nofollow">
<title>LoyPOS — install &amp; migrate</title>
<style>
 body{font-family:system-ui,-apple-system,Segoe UI,Roboto,sans-serif;background:#0f172a;color:#e2e8f0;margin:0;padding:2rem;}
 .card{max-width:720px;margin:0 auto;background:#1e293b;border-radius:12px;padding:2rem;box-shadow:0 10px 30px rgba(0,0,0,.4);}
 h1{font-size:1.3rem;margin:0 0 .3rem;} .sub{color:#94a3b8;font-size:.9rem;margin-bottom:1.5rem;}
 label{display:block;margin:.9rem 0 .3rem;font-size:.9rem;color:#cbd5e1;}
 input[type=text],input[type=password]{width:100%;box-sizing:border-box;padding:.6rem .7rem;border-radius:8px;border:1px solid #334155;background:#0f172a;color:#e2e8f0;font-size:.95rem;}
 .row{display:flex;gap:1rem;} .row>div{flex:1;}
 .check{display:flex;align-items:center;gap:.5rem;margin-top:1rem;font-size:.9rem;color:#cbd5e1;}
 button{margin-top:1.4rem;width:100%;padding:.75rem;border:0;border-radius:8px;background:#2563eb;color:#fff;font-size:1rem;cursor:pointer;}
 button:hover{background:#1d4ed8;}
 table{width:100%;border-collapse:collapse;margin-top:1rem;font-size:.9rem;}
 th,td{text-align:left;padding:.45rem .6rem;border-bottom:1px solid #334155;}
 th{color:#94a3b8;font-weight:600;}
 .ok{color:#4ade80;} .warn{color:#facc15;} .err{color:#f87171;}
 .token{background:#0f172a;border:1px dashed #475569;padding:.8rem 1rem;border-radius:8px;margin-top:1rem;font-family:ui-monospace,monospace;font-size:.85rem;word-break:break-all;}
 .note{color:#94a3b8;font-size:.8rem;margin-top:1rem;line-height:1.5;}
 code{color:#7dd3fc;}
</style>
</head>
<body>
<div class="card">
 <h1>LoyPOS — database installer</h1>
 <div class="sub">novelsapps.com.et &middot; httpdocs/api/v1 &middot; idempotent migration, safe on databases that already contain data</div>

<?php if ($done): ?>
 <table>
  <tr><th>Check</th><th>Result</th></tr>
  <?php foreach ($results as [$s, $m, $st, $d]): ?>
   <tr><td><?= e($s) ?></td><td><span class="<?= $st ?>"><?= e($m) ?></span><?= $d !== '' ? '<br><span style="color:#64748b;font-size:.8rem">' . e($d) . '</span>' : '' ?></td></tr>
  <?php endforeach; ?>
 </table>

 <?php $shown = $countsAfter ?: $countsBefore; if ($shown): ?>
 <table>
  <tr><th>Table</th><th>Rows (before)</th><th>Rows (now)</th></tr>
  <?php foreach ($tables as $t): if ($t === 'schema_migrations') continue; ?>
   <tr><td><?= e($t) ?></td>
       <td><?= $countsBefore && $countsBefore[$t] !== null ? (int)$countsBefore[$t] : '—' ?></td>
       <td><?= $shown[$t] !== null ? (int)$shown[$t] : '<span class="warn">missing</span>' ?></td></tr>
  <?php endforeach; ?>
 </table>
 <?php endif; ?>

 <?php if ($generatedToken !== ''): ?>
  <div class="token">New sync_token (copy it now, it is stored in config.php):<br><strong><?= e($generatedToken) ?></strong><br>
  <span class="note">Set this same value as <code>PLESK_SYNC_TOKEN</code> in the AI Studio Secrets panel so the LoyPOS app can authenticate.</span></div>
 <?php elseif ($tokenKept): ?>
  <div class="token">sync_token: kept unchanged from the <code>config.php</code> shipped in the bundle (no need to update <code>PLESK_SYNC_TOKEN</code> unless you typed a new one).</div>
 <?php endif; ?>

 <p class="note">Next steps: check <code>https://novelsapps.com.et/api/v1/health</code> with header
 <code>Authorization: Bearer &lt;sync_token&gt;</code>, then <strong>delete <code>install.php</code></strong> via Plesk File Manager.
 The generated <code>config.php</code> stays in <code>httpdocs/api/v1/</code>.</p>

<?php else: ?>
 <?php if ($locked): ?><p class="note" style="color:#facc15">install.lock exists — a previous migration already ran. To re-run, delete <code>httpdocs/api/v1/install.lock</code> in File Manager.</p><?php endif; ?>
 <form method="post">
  <div class="row">
   <div><label>Database host</label><input type="text" name="db_host" value="<?= e($_POST['db_host'] ?? ($cfgExisting['db_host'] ?? 'localhost')) ?>"></div>
   <div><label>Database port</label><input type="text" name="db_port" value="<?= e($_POST['db_port'] ?? (string)($cfgExisting['db_port'] ?? '3306')) ?>"></div>
  </div>
  <div class="row">
   <div><label>Database name</label><input type="text" name="db_name" placeholder="e.g. loypos" value="<?= e($_POST['db_name'] ?? ($cfgExisting['db_name'] ?? '')) ?>" <?= empty($cfgExisting['db_name']) ? 'required' : '' ?>></div>
   <div><label>Database user</label><input type="text" name="db_user" placeholder="as shown in Plesk" value="<?= e($_POST['db_user'] ?? ($cfgExisting['db_user'] ?? '')) ?>" <?= empty($cfgExisting['db_user']) ? 'required' : '' ?>></div>
  </div>
  <label>Database password<?= $cfgExisting && !empty($cfgExisting['db_pass']) ? ' (leave empty to reuse the saved one)' : '' ?></label>
  <input type="password" name="db_pass" placeholder="<?= $cfgExisting && !empty($cfgExisting['db_pass']) ? '•••• saved in config.php ••••' : 'the one you set in Plesk → Add database' ?>" <?= empty($cfgExisting['db_pass']) ? 'required' : '' ?>>
  <label>Sync token<?= $cfgExisting && !empty($cfgExisting['sync_token']) ? ' (leave empty to keep the saved one)' : ' (leave empty to auto-generate a strong one)' ?></label>
  <input type="text" name="sync_token" placeholder="<?= $cfgExisting && !empty($cfgExisting['sync_token']) ? 'keep saved token' : 'auto-generate' ?>" value="<?= e($_POST['sync_token'] ?? '') ?>">
  <div class="check"><input type="checkbox" name="mode" value="report" id="ro"><label for="ro" style="margin:0">Report only — connect and show current data, change nothing</label></div>
  <div class="check"><input type="checkbox" name="overwrite_config" id="oc"><label for="oc" style="margin:0">Overwrite existing config.php (if present)</label></div>
  <button type="submit"><?= $locked ? 'Report current state' : 'Test connection &amp; run migration' ?></button>
 </form>
 <p class="note">This installer only creates missing tables (<code>CREATE TABLE IF NOT EXISTS</code>) and never drops, truncates or alters existing data.
 Create the database first: <strong>Plesk → Databases → Add database</strong> (Plesk prefixes names with <code>novelsap_</code>).
 After success the app API lives at <code>/api/v1/health</code>, <code>/api/v1/pull</code>, <code>/api/v1/push</code>.</p>
<?php endif; ?>
</div>
</body>
</html>
