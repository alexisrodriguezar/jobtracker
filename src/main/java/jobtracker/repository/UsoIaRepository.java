package jobtracker.repository;

import jobtracker.entity.UsoIa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface UsoIaRepository extends JpaRepository<UsoIa, Long> {

    @Query("SELECT COALESCE(SUM(u.cantidad), 0L) FROM UsoIa u WHERE u.fecha = :fecha")
    long totalDelDia(@Param("fecha") LocalDate fecha);

    @Query("SELECT COALESCE(SUM(u.cantidad), 0L) FROM UsoIa u "
            + "WHERE u.usuario.id = :usuarioId AND u.fecha = :fecha")
    long totalDelUsuarioEnElDia(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDate fecha);

    @Modifying
    @Query(value = """
            INSERT INTO uso_ia (usuario_id, fecha, cantidad)
            VALUES (:usuarioId, :fecha, 1)
            ON DUPLICATE KEY UPDATE cantidad = cantidad + 1
            """, nativeQuery = true)
    void registrarUso(@Param("usuarioId") Long usuarioId, @Param("fecha") LocalDate fecha);
}