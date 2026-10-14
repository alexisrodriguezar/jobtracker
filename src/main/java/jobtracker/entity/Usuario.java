package jobtracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "usuario_id")
    private Long id;

    @Column (name = "nombre", nullable = false)
    private String nombre;

    @Column (name = "apellido", nullable = false)
    private String apellido;

    @Column (name = "email", nullable = false, unique = true)
    private String email;

    @Column (name = "password_hash", nullable = false)
    private String passwordHash;
}
