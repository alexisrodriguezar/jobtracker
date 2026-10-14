package jobtracker.repository;

import jobtracker.entity.Estado;
import jobtracker.entity.Postulacion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostulacionRepository extends JpaRepository<Postulacion, Long> {

    Page<Postulacion> findByUsuarioId(Long usuarioId, Pageable pageable);

    Page<Postulacion> findByUsuarioIdAndEstado(Long usuarioId, Estado estado, Pageable pageable);

    Optional<Postulacion> findByIdAndUsuarioId(Long id, Long usuarioId);
}

