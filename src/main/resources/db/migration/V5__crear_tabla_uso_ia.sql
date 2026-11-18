CREATE TABLE uso_ia (
    uso_id     BIGINT NOT NULL AUTO_INCREMENT,
    usuario_id BIGINT NOT NULL,
    fecha      DATE   NOT NULL,
    cantidad   INT    NOT NULL DEFAULT 0,
    PRIMARY KEY (uso_id),
    CONSTRAINT uk_uso_ia_usuario_fecha UNIQUE (usuario_id, fecha),
    CONSTRAINT fk_uso_ia_usuario FOREIGN KEY (usuario_id) REFERENCES usuario (usuario_id)
) ENGINE = InnoDB;