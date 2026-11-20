ALTER TABLE postulaciones
    ADD COLUMN analisis_json  VARCHAR(4000),
    ADD COLUMN analisis_fecha DATETIME;