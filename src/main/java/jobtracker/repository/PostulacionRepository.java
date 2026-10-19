package jobtracker.repository;

import jobtracker.entity.Estado;
import jobtracker.entity.Postulacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    Page<Postulacion> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<Postulacion> findByUsuarioIdAndEstado(Long usuarioId, Estado estado, Pageable pageable);

    Optional<Postulacion> findByIdAndUsuarioId(Long id, Long usuarioId);

    @EntityGraph(attributePaths = "empresa")
    @Query("""
        SELECT p FROM Postulacion p JOIN p.empresa e
        WHERE p.usuario.id = :usuarioId
          AND (:estado IS NULL OR p.estado = :estado)
          AND (:buscar IS NULL OR LOWER(p.puesto) LIKE LOWER(CONCAT('%', :buscar, '%'))
                               OR LOWER(e.nombre) LIKE LOWER(CONCAT('%', :buscar, '%')))
        """)
    Page<Postulacion> buscar(@Param("usuarioId") Long usuarioId,
                             @Param("estado") Estado estado,
                             @Param("buscar") String buscar,
                             Pageable pageable);
}

