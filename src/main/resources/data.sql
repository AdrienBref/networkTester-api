-- =========================================================
--  DATOS INICIALES — Versión adaptada a nuevo modelo
-- =========================================================

-- === Dispositivos base ===
INSERT INTO device_entity (
    id, 
    name, 
    ip, 
    ping_interval, 
    test_always, 
    min_offline_alarm, 
    start_time, 
    end_time
) VALUES
    (RANDOM_UUID(), 'Mozilla',        'mozilla.org',       1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'GitHub',         'github.com',        1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Stack Overflow', 'stackoverflow.com', 1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Example',        'example.com',       1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Cloudflare',     'cloudflare.com',    1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Amazon',         'amazon.com',        1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'YouTube',        'youtube.com',       1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'X',              'x.com',             1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Facebook',       'facebook.com',      1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Reddit',         'reddit.com',        1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'Wikipedia',      'wikipedia.org',     1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'LinkedIn',       'linkedin.com',      1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59'),
    (RANDOM_UUID(), 'PayPal',         'paypal.com',        1000, TRUE, 1, TIME '00:00:00', TIME '23:59:59');

-- =========================================================
--  ASIGNACIÓN DE DÍAS DE NOTIFICACIÓN (tabla secundaria)
-- =========================================================
--  Cada dispositivo tiene sus días permitidos.
--  Para simplificar, aplicamos L–V (lunes a viernes) a todos.
-- =========================================================

INSERT INTO device_notify_day (device_id, day_of_week)
SELECT id, 'MONDAY' FROM device_entity;
INSERT INTO device_notify_day (device_id, day_of_week)
SELECT id, 'TUESDAY' FROM device_entity;
INSERT INTO device_notify_day (device_id, day_of_week)
SELECT id, 'WEDNESDAY' FROM device_entity;
INSERT INTO device_notify_day (device_id, day_of_week)
SELECT id, 'THURSDAY' FROM device_entity;
INSERT INTO device_notify_day (device_id, day_of_week)
SELECT id, 'FRIDAY' FROM device_entity;

INSERT INTO email_entity (id, email) VALUES
('8fbd8b1d-0f85-4d8f-9201-7d9c80e648ae', 'soporte@homiyummy.com'),
('76b8d4e3-70d1-46cd-9df8-f92b7b15f1a0', 'alertas@homiyummy.com'),
('d23aa083-4f5a-4c33-bd55-bda43a9ef3d2', 'developadri@gmail.com'),
('a640de1f-2dfb-46a5-986d-9aef8c5da47e', 'backups@homiyummy.com'),
('20a68842-b727-4ac8-9fdc-95d4b7d6b0a7', 'admin@homiyummy.com');

