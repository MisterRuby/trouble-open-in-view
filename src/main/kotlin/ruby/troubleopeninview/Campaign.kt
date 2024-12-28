package ruby.troubleopeninview

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service

@Entity
class Campaign(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = true)
    @JsonIgnoreProperties("campaign")
    val contracts: MutableList<Contract> = mutableListOf()
)

interface CampaignRepository : JpaRepository<Campaign, Long>

@Service
class CampaignService(val campaignRepository: CampaignRepository) {

    fun getAllCampaigns(): List<Campaign> {
        return campaignRepository.findAll()
    }
}
