package ruby.troubleopeninview

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Target(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val name: String
)

interface TargetRepository : JpaRepository<Target, Long>
