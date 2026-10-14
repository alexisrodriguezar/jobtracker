package jobtracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table (name = "empresa")
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column (name = "empresa_id")
    private Long id;

    @Column(name = "nombre", nullable = false, unique = true)
    private String nombre;
}
