CREATE TABLE usuario (
    usuario_id    BIGINT       NOT NULL AUTO_INCREMENT,
    nombre        VARCHAR(255) NOT NULL,
    apellido      VARCHAR(255) NOT NULL,
    email         VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    PRIMARY KEY (usuario_id),
    CONSTRAINT uk_usuario_email UNIQUE (email)
) ENGINE = InnoDB;

CREATE TABLE empresa (
    empresa_id BIGINT       NOT NULL AUTO_INCREMENT,
    nombre     VARCHAR(255) NOT NULL,
    PRIMARY KEY (empresa_id),
    CONSTRAINT uk_empresa_nombre UNIQUE (nombre)
) ENGINE = InnoDB;

CREATE TABLE postulaciones (
    postulacion_id    BIGINT       NOT NULL AUTO_INCREMENT,
    puesto            VARCHAR(255) NOT NULL,
    estado            ENUM ('ENVIADA', 'ENTREVISTA', 'RECHAZADA', 'OFERTA') NOT NULL,
    seniority         ENUM ('TRAINEE', 'JUNIOR', 'SEMI_SENIOR', 'SENIOR'),
    link              VARCHAR(255),
    nota              VARCHAR(1000),
    fecha_postulacion DATE,
    usuario_id        BIGINT       NOT NULL,
    empresa_id        BIGINT       NOT NULL,
    PRIMARY KEY (postulacion_id),
    CONSTRAINT fk_postulaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (usuario_id),
    CONSTRAINT fk_postulaciones_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (empresa_id)
) ENGINE = InnoDB;
