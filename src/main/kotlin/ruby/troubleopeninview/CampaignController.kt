package ruby.troubleopeninview

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/campaigns")
class CampaignController(
    private val campaignService: CampaignService
) {

    @GetMapping
    fun getAllCampaigns(): List<Campaign> {
        return campaignService.getAllCampaigns()
    }
}
