package ruby.troubleopeninview

import jakarta.transaction.Transactional
import org.springframework.boot.CommandLineRunner
import org.springframework.stereotype.Component
import kotlin.random.Random

@Component
class TestDataInitializer(
    private val campaignRepository: CampaignRepository,
    private val contractRepository: ContractRepository,
    private val targetRepository: TargetRepository,
    private val contractTargetRepository: ContractTargetRepository
) : CommandLineRunner {

    private val targetNames = listOf(
        "서울", "대전", "대구", "부산", "인천", "부천", "세종", "광주", "제주도", "여수"
    )

    @Transactional
    override fun run(vararg args: String?) {
        // 타겟 생성
        val targets = targetNames.map { name ->
            targetRepository.save(Target(name = name))
        }

        // 캠페인 생성
        repeat(10) { campaignIndex ->
            val campaign = Campaign(name = "캠페인 $campaignIndex")
            campaignRepository.save(campaign)

            // 청약 생성
            repeat(10) { contractIndex ->
                val contract = Contract(title = "캠페인 $campaignIndex - 청약 $contractIndex", campaign = campaign)
                contractRepository.save(contract)

                // 청약당 랜덤 타겟 3~5개 생성
                val randomTargets = targets.shuffled().take(Random.nextInt(3, 6))
                randomTargets.forEach { target ->
                    contractTargetRepository.save(ContractTarget(contract = contract, target = target))
                }
            }

            campaignRepository.save(campaign) // 캠페인 저장
        }

        println("테스트 데이터 생성 완료!")
        println("캠페인 생성 : ${campaignRepository.count()} 개")
        println("타겟 생성 : ${targetRepository.count()} 개")
        println("청약 생성 : ${contractRepository.count()} 개")
        println("청약 타겟 생성 : ${contractTargetRepository.count()} 개")
    }
}
