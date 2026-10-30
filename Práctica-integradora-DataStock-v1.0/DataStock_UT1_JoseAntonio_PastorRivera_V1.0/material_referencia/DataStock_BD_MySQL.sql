-- DataStock: esquema MySQL 8.0.16+.
-- Idempotente: puede ejecutarse repetidas veces; no borra datos existentes.
-- NOTA: CREATE TABLE IF NOT EXISTS no modifica tablas ya existentes.
CREATE DATABASE IF NOT EXISTS datastock CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE datastock;

-- Catálogo de categorías: identificador y nombre únicos.
CREATE TABLE IF NOT EXISTS categoria (
  id INT NOT NULL PRIMARY KEY,
  nombre VARCHAR(60) NOT NULL UNIQUE
) ENGINE=InnoDB;

-- Proveedores; email opcional y único cuando se informa.
CREATE TABLE IF NOT EXISTS proveedor (
  id INT NOT NULL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  email VARCHAR(120) UNIQUE
) ENGINE=InnoDB;

-- Productos: se impide precio no positivo y stock negativo.
CREATE TABLE IF NOT EXISTS producto (
  codigo VARCHAR(12) NOT NULL PRIMARY KEY,
  nombre VARCHAR(100) NOT NULL,
  precio DECIMAL(10,2) NOT NULL,
  stock INT NOT NULL DEFAULT 0,
  categoria_id INT NOT NULL,
  proveedor_id INT NOT NULL,
  CONSTRAINT ck_producto_precio CHECK (precio > 0),
  CONSTRAINT ck_producto_stock CHECK (stock >= 0),
  CONSTRAINT ck_producto_codigo CHECK (codigo REGEXP '^[A-Z0-9-]{1,12}$'),
  CONSTRAINT ck_producto_nombre CHECK (CHAR_LENGTH(nombre) BETWEEN 3 AND 100),
  CONSTRAINT fk_producto_categoria FOREIGN KEY (categoria_id) REFERENCES categoria(id),
  CONSTRAINT fk_producto_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedor(id)
) ENGINE=InnoDB;

-- Historial de movimientos: no se borra en cascada al borrar productos.
CREATE TABLE IF NOT EXISTS movimiento_stock (
  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
  producto_codigo VARCHAR(12) NOT NULL,
  tipo VARCHAR(7) NOT NULL,
  cantidad INT NOT NULL,
  fecha DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  stock_anterior INT NOT NULL,
  stock_posterior INT NOT NULL,
  CONSTRAINT ck_mov_tipo CHECK (tipo IN ('ENTRADA','SALIDA')),
  CONSTRAINT ck_mov_cantidad CHECK (cantidad > 0),
  CONSTRAINT ck_mov_stock_anterior CHECK (stock_anterior >= 0),
  CONSTRAINT ck_mov_stock_posterior CHECK (stock_posterior >= 0),
  CONSTRAINT fk_mov_producto FOREIGN KEY (producto_codigo) REFERENCES producto(codigo)
) ENGINE=InnoDB;

-- Procedimiento de salida/entrada de stock: transacción atómica.
-- DROP + CREATE permite actualizarlo en sucesivas ejecuciones del script.
-- Se evita sobrescribir datos de tablas existentes.
DROP PROCEDURE IF EXISTS registrar_movimiento;
DELIMITER $$
CREATE PROCEDURE registrar_movimiento(
 IN p_codigo VARCHAR(12), IN p_tipo VARCHAR(7), IN p_cantidad INT
)
BEGIN
 DECLARE v_stock INT DEFAULT NULL;
 DECLARE v_nuevo INT;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
   ROLLBACK;
   RESIGNAL;
 END;
 IF p_tipo NOT IN ('ENTRADA','SALIDA') OR p_tipo IS NULL OR p_cantidad IS NULL OR p_cantidad <= 0 THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Tipo o cantidad invalidos';
 END IF;
 START TRANSACTION;
 SELECT stock INTO v_stock FROM producto WHERE codigo=p_codigo FOR UPDATE;
 IF v_stock IS NULL THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Producto inexistente';
 END IF;
 SET v_nuevo = IF(p_tipo='ENTRADA',v_stock+p_cantidad,v_stock-p_cantidad);
 IF v_nuevo < 0 THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='Stock insuficiente';
 END IF;
 UPDATE producto SET stock=v_nuevo WHERE codigo=p_codigo;
 INSERT INTO movimiento_stock(producto_codigo,tipo,cantidad,stock_anterior,stock_posterior)
 VALUES(p_codigo,p_tipo,p_cantidad,v_stock,v_nuevo);
 COMMIT;
END$$
DELIMITER ;
-- Ejemplo (una vez existan productos): CALL registrar_movimiento('TEC-001','ENTRADA',5);
