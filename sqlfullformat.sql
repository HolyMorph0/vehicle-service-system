/*
  FLEET SERVICE DATABASE MANAGEMENT SYSTEM
*/
-- 1. VERİTABANI KURULUMU VE AYARLAR
DROP DATABASE IF EXISTS fleet_service_db;
CREATE DATABASE fleet_service_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
USE fleet_service_db;

SET sql_safe_updates = 0;
SET FOREIGN_KEY_CHECKS = 0;

-- 2. TABLO OLUŞTURMA (TABLES)

-- 2.1. Araç Tablosu (Vehicle)
CREATE TABLE vehicle (
  vehicle_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  plate_no VARCHAR(15) NOT NULL,
  vin_no   VARCHAR(32) NOT NULL,
  make     VARCHAR(50) NOT NULL,
  model    VARCHAR(50) NOT NULL,
  model_year SMALLINT NOT NULL,
  colour   VARCHAR(30) NULL,
  current_km INT UNSIGNED NOT NULL DEFAULT 0,
  status ENUM('ACTIVE','IN_SERVICE','ASSIGNED','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
  notes VARCHAR(500) NULL,
  service_entry_date DATE NULL,
  UNIQUE KEY uq_vehicle_plate (plate_no),
  UNIQUE KEY uq_vehicle_vin (vin_no),
  CONSTRAINT chk_vehicle_year CHECK (model_year BETWEEN 1980 AND 2100)
) ENGINE=InnoDB;

-- 2.2. Sürücü Tablosu (Drivers)
CREATE TABLE drivers (
  driver_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  first_name VARCHAR(50) NOT NULL,
  last_name  VARCHAR(50) NOT NULL,
  license_no VARCHAR(30) NOT NULL,
  phone      VARCHAR(25) NULL,
  UNIQUE KEY uq_driver_license (license_no)
) ENGINE=InnoDB;

-- 2.3. Bakım Tablosu (Maintenance)
CREATE TABLE maintenance (
  maint_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  maint_date DATE NOT NULL,
  maint_type VARCHAR(50) NOT NULL,
  odometer_km INT UNSIGNED NOT NULL,
  description VARCHAR(500) NULL,
  cost DECIMAL(10,2) NOT NULL DEFAULT 0.00,
  vehicle_id INT UNSIGNED NOT NULL,
  KEY ix_maintenance_vehicle (vehicle_id),
  KEY ix_maintenance_date (maint_date),
  CONSTRAINT fk_maint_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
) ENGINE=InnoDB;

-- 2.4. Görev Atama Tablosu (Assignment)
CREATE TABLE assignment (
  assignment_id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
  start_km INT UNSIGNED NOT NULL,
  end_km   INT UNSIGNED NULL,
  start_datetime DATETIME NOT NULL,
  end_datetime   DATETIME NULL,
  driver_id  INT UNSIGNED NOT NULL,
  vehicle_id INT UNSIGNED NOT NULL,

  -- Aktif görev kontrolü için Generated Columns
  active_vehicle_id INT UNSIGNED
    GENERATED ALWAYS AS (IF(end_datetime IS NULL, vehicle_id, NULL)) STORED,
  active_driver_id  INT UNSIGNED
    GENERATED ALWAYS AS (IF(end_datetime IS NULL, driver_id, NULL)) STORED,

  KEY ix_asg_driver  (driver_id),
  KEY ix_asg_vehicle (vehicle_id),
  KEY ix_asg_startdt (start_datetime),

  UNIQUE KEY uq_active_vehicle (active_vehicle_id),
  UNIQUE KEY uq_active_driver  (active_driver_id),

  CONSTRAINT chk_asg_end_after_start
    CHECK (end_datetime IS NULL OR end_datetime >= start_datetime),
  CONSTRAINT chk_asg_endkm_after_startkm
    CHECK (end_km IS NULL OR end_km >= start_km),

  CONSTRAINT fk_asg_driver
    FOREIGN KEY (driver_id) REFERENCES drivers(driver_id)
    ON DELETE RESTRICT,

  CONSTRAINT fk_asg_vehicle
    FOREIGN KEY (vehicle_id) REFERENCES vehicle(vehicle_id)
    ON DELETE RESTRICT
) ENGINE=InnoDB;

SET FOREIGN_KEY_CHECKS = 1;

-- 3. RAPORLAMA KATMANI (VIEWS)

-- 3.1. Java AppUI: Aktif Görevler Detaylı Raporu
CREATE OR REPLACE VIEW vw_active_assignments_detailed AS
SELECT
  a.assignment_id,
  a.start_datetime,
  a.start_km,
  TIMESTAMPDIFF(MINUTE, a.start_datetime, NOW()) AS active_minutes,
  a.driver_id,
  CONCAT(d.first_name, ' ', d.last_name) AS driver_name,
  d.license_no,
  a.vehicle_id,
  v.plate_no,
  v.vin_no,
  v.make,
  v.model,
  v.status
FROM assignment a
JOIN drivers d ON d.driver_id = a.driver_id
JOIN vehicle v  ON v.vehicle_id = a.vehicle_id
WHERE a.end_datetime IS NULL;

-- 3.2. Java AppUI: Araç Başına Toplam Bakım Maliyeti
CREATE OR REPLACE VIEW vw_vehicle_maintenance_summary AS
SELECT
  v.vehicle_id,
  v.plate_no,
  v.make,
  v.model,
  COUNT(m.maint_id) AS maint_count,
  COALESCE(SUM(m.cost), 0.00) AS total_cost,
  MAX(m.maint_date) AS last_maint_date
FROM vehicle v
LEFT JOIN maintenance m ON m.vehicle_id = v.vehicle_id
GROUP BY v.vehicle_id, v.plate_no, v.make, v.model;

-- 3.3. Java AppUI: Detaylı Bakım Geçmişi
CREATE OR REPLACE VIEW vw_maintenance_history AS
SELECT 
    m.maint_id,
    m.maint_date,
    m.maint_type,
    m.odometer_km,
    m.cost,
    m.description,
    v.vehicle_id,
    v.plate_no,
    v.make,
    v.model
FROM maintenance m
JOIN vehicle v ON m.vehicle_id = v.vehicle_id
ORDER BY m.maint_date DESC;

-- 4. EKSTRA GÖRÜNÜMLER 

-- 4.1. Sadece "Müsait" (Boşta) Olan Araçlar
CREATE OR REPLACE VIEW vw_available_vehicles AS
SELECT v.vehicle_id, v.plate_no, v.make, v.model, v.current_km
FROM vehicle v
WHERE v.status = 'ACTIVE'
  AND NOT EXISTS (
    SELECT 1 FROM assignment a WHERE a.vehicle_id = v.vehicle_id AND a.end_datetime IS NULL
  );

-- 4.2. Sadece "Müsait" (Boşta) Olan Sürücüler
CREATE OR REPLACE VIEW vw_available_drivers AS
SELECT d.driver_id, d.first_name, d.last_name, d.license_no
FROM drivers d
WHERE NOT EXISTS (
  SELECT 1 FROM assignment a WHERE a.driver_id = d.driver_id AND a.end_datetime IS NULL
  );

-- 4.3. Detaylı (Driver -> Active Vehicle)
CREATE OR REPLACE VIEW vw_driver_active_vehicle AS
SELECT
  d.driver_id, d.first_name, d.last_name, d.license_no,
  CASE WHEN a.assignment_id IS NULL THEN NULL ELSE CONCAT(v.plate_no, ' - ', v.make, ' ', v.model) END AS active_vehicle
FROM drivers d
LEFT JOIN assignment a ON a.driver_id = d.driver_id AND a.end_datetime IS NULL
LEFT JOIN vehicle v ON v.vehicle_id = a.vehicle_id;

-- 5. İŞ MANTIĞI (TRIGGERS & PROCEDURES)

DELIMITER $$

-- Trigger: Bakım KM kontrolü
CREATE TRIGGER trg_maintenance_km_check
BEFORE INSERT ON maintenance
FOR EACH ROW
BEGIN
  DECLARE v_km INT UNSIGNED;
  SELECT current_km INTO v_km FROM vehicle WHERE vehicle_id = NEW.vehicle_id;
  IF v_km IS NOT NULL AND NEW.odometer_km < v_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Maintenance odometer cannot be less than vehicle current KM';
  END IF;
END $$

-- Trigger: Bakım sonrası araç KM güncelleme
CREATE TRIGGER trg_maintenance_sync_vehicle_km
AFTER INSERT ON maintenance
FOR EACH ROW
BEGIN
  UPDATE vehicle
  SET current_km = GREATEST(current_km, NEW.odometer_km)
  WHERE vehicle_id = NEW.vehicle_id;
END $$

-- Procedure: GÜVENLİ Görev Atama
CREATE PROCEDURE sp_create_assignment(
  IN p_driver_id INT UNSIGNED,
  IN p_vehicle_id INT UNSIGNED,
  IN p_start_km INT UNSIGNED,
  IN p_start_datetime DATETIME
)
BEGIN
  DECLARE v_vehicle_km INT UNSIGNED;
  DECLARE v_status VARCHAR(20);

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  -- 1. Driver kontrolü
  IF (SELECT COUNT(*) FROM drivers WHERE driver_id = p_driver_id) = 0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Driver not found';
  END IF;

  -- 2. Vehicle kontrolü ve kilitleme
  SELECT current_km, status INTO v_vehicle_km, v_status
  FROM vehicle WHERE vehicle_id = p_vehicle_id FOR UPDATE;

  IF v_vehicle_km IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle not found';
  END IF;

  -- 3. Çakışma Kontrolleri
  IF EXISTS (SELECT 1 FROM assignment WHERE driver_id = p_driver_id AND end_datetime IS NULL LIMIT 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Driver already has an active assignment';
  END IF;

  IF EXISTS (SELECT 1 FROM assignment WHERE vehicle_id = p_vehicle_id AND end_datetime IS NULL LIMIT 1) THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle already has an active assignment';
  END IF;

  -- 4. Durum ve KM Kontrolleri
  IF v_status <> 'ACTIVE' THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Vehicle is not ACTIVE; cannot create assignment';
  END IF;

  IF p_start_km < v_vehicle_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Start KM cannot be less than vehicle current KM';
  END IF;

  -- 5. İşlem
  INSERT INTO assignment (driver_id, vehicle_id, start_km, start_datetime, end_km, end_datetime)
  VALUES (p_driver_id, p_vehicle_id, p_start_km, p_start_datetime, NULL, NULL);

  UPDATE vehicle SET status = 'ASSIGNED', current_km = p_start_km WHERE vehicle_id = p_vehicle_id;

  COMMIT;
END $$

-- Procedure: GÜVENLİ Görev Bitirme
CREATE PROCEDURE sp_close_assignment(
  IN p_assignment_id INT UNSIGNED,
  IN p_end_km INT UNSIGNED,
  IN p_end_datetime DATETIME
)
BEGIN
  DECLARE v_vehicle_id INT UNSIGNED;
  DECLARE v_start_km INT UNSIGNED;
  DECLARE v_end_dt DATETIME;

  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  START TRANSACTION;

  SELECT vehicle_id, start_km, end_datetime INTO v_vehicle_id, v_start_km, v_end_dt
  FROM assignment WHERE assignment_id = p_assignment_id FOR UPDATE;

  IF v_vehicle_id IS NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment not found';
  END IF;

  IF v_end_dt IS NOT NULL THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Assignment already closed';
  END IF;

  IF p_end_km < v_start_km THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'End KM cannot be less than Start KM';
  END IF;

  UPDATE assignment SET end_km = p_end_km, end_datetime = p_end_datetime WHERE assignment_id = p_assignment_id;
  UPDATE vehicle SET status = 'ACTIVE', current_km = p_end_km WHERE vehicle_id = v_vehicle_id;

  COMMIT;
END $$

DELIMITER ;

-- 6. TEST VERİLERİ (SEED DATA)

-- Araçlar
INSERT INTO vehicle (plate_no, vin_no, make, model, model_year, colour, current_km, status, notes) VALUES
('34ABC123', 'VIN001', 'BMW', '320i', 2018, 'Black', 119000, 'ACTIVE', 'Demo Car 1'),
('06XYZ789', 'VIN002', 'BMW', '520d', 2016, 'White', 183000, 'IN_SERVICE', 'Demo Car 2'),
('35KLM456', 'VIN003', 'Mercedes', 'C200', 2021, 'Grey', 45000, 'ACTIVE', 'VP Vehicle');

-- Sürücüler
INSERT INTO drivers (first_name, last_name, license_no, phone) VALUES
('Ali', 'Yilmaz', 'TR001', '5551112233'),
('Veli', 'Ozturk', 'TR002', '5554445566'),
('Ayse', 'Demir', 'TR003', '5557778899');

-- Bakım Geçmişi
INSERT INTO maintenance (maint_date, maint_type, odometer_km, description, cost, vehicle_id) VALUES
('2025-12-01', 'Oil Change', 119500, 'Regular maintenance', 2500.00, 1),
('2025-12-10', 'Brake Pads', 184000, 'Front brakes replaced', 4500.00, 2);

-- Örnek Görev Atamaları
-- Ali Yılmaz -> BMW 320i (120k KM ile başlat)
CALL sp_create_assignment(1, 1, 120000, NOW());

-- Veli Öztürk -> Mercedes C200
CALL sp_create_assignment(2, 3, 45000, NOW());