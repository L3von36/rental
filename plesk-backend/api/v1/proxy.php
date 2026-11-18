<?php
/**
 * LoyPOS Professional Server-Side Proxy Example (proxy.php)
 * 
 * BEST PRACTICE:
 * In professional production architecture, mobile apps never connect directly to databases
 * or embed raw master database credentials. Instead, requests are routed through a secure
 * Server-Side Proxy or API Gateway that validates user sessions (JWT/OAuth), signs requests,
 * and interacts with MySQL/MariaDB securely.
 */

header('Content-Type: application/json; charset=utf-8');

if (!file_exists(__DIR__ . '/config.php')) {
    http_response_code(503);
    echo json_encode(['error' => 'Server not configured: copy config.example.php to config.php']);
    exit;
}

// config.php returns the same array used by index.php
$config = require __DIR__ . '/config.php';

// 1. Authenticate Request via Bearer Token or App Check Signature
$headers = getallheaders();
$authHeader = $headers['Authorization'] ?? $headers['authorization'] ?? '';
$syncTokenHeader = $headers['X-Sync-Token'] ?? $headers['x-sync-token'] ?? '';

$token = '';
if (preg_match('/Bearer\s(\S+)/', $authHeader, $matches)) {
    $token = $matches[1];
} elseif (!empty($syncTokenHeader)) {
    $token = $syncTokenHeader;
}

$expectedToken = (string)($config['sync_token'] ?? '');
if ($expectedToken === '' || !hash_equals($expectedToken, $token)) {
    http_response_code(401);
    echo json_encode(['error' => 'Unauthorized: Invalid or missing secure sync token']);
    exit;
}

// 2. Forward request or handle business logic securely via PDO
try {
    $pdo = new PDO(
        sprintf(
            'mysql:host=%s;port=%d;dbname=%s;charset=utf8mb4',
            (string)($config['db_host'] ?? 'localhost'),
            (int)($config['db_port'] ?? 3306),
            (string)($config['db_name'] ?? '')
        ),
        (string)($config['db_user'] ?? ''),
        (string)($config['db_pass'] ?? ''),
        [
            PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
            PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
        ]
    );

    $action = $_GET['action'] ?? 'health';
    if ($action === 'health') {
        echo json_encode(['status' => 'healthy', 'proxy' => 'active', 'server_time_ms' => round(microtime(true) * 1000)]);
        exit;
    }

    echo json_encode(['status' => 'ok', 'message' => 'Proxy request handled successfully']);
} catch (Exception $e) {
    http_response_code(503);
    echo json_encode(['error' => 'Database proxy connection error: ' . $e->getMessage()]);
}
