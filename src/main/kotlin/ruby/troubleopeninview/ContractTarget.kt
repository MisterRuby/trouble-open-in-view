package ruby.troubleopeninview

import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class ContractTarget(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne
    @JoinColumn(name = "contact_id")
    val contract: Contract,

    @ManyToOne
    @JoinColumn(name = "target_id")
    val target: Target
)

interface ContractTargetRepository : JpaRepository<ContractTarget, Long>
