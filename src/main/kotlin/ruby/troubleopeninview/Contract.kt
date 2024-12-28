package ruby.troubleopeninview

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository

@Entity
class Contract(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val title: String,

    @ManyToOne
    @JoinColumn(name = "campaign_id")
    val campaign: Campaign,

    @OneToMany(mappedBy = "contract", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnoreProperties("contract")
    val targets: MutableList<ContractTarget> = mutableListOf()
)

interface ContractRepository : JpaRepository<Contract, Long>
