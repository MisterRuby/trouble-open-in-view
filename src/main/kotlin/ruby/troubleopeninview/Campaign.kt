package ruby.troubleopeninview

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import jakarta.persistence.*
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Service
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Entity
class Campaign(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @Column(nullable = false)
    val name: String,

    @OneToMany(mappedBy = "campaign", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties("campaign")
    val contracts: MutableList<Contract> = mutableListOf()
)

data class CampaignResponse(val id: Long, val name: String)

interface CampaignRepository : JpaRepository<Campaign, Long>

@Service
class CampaignService(val campaignRepository: CampaignRepository) {

    fun getAllCampaigns(): List<CampaignResponse> {
        return campaignRepository.findAll().map { campaign ->
            CampaignResponse(campaign.id!!, campaign.name)
        }
    }
}

@RestController
@RequestMapping("/campaigns")
class CampaignController(
    private val campaignService: CampaignService
) {

    @GetMapping
    fun getAllCampaigns(): List<CampaignResponse> {
        return campaignService.getAllCampaigns()
    }
}
