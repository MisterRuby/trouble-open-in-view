# [Spring] JPA 에서 open-in-view 를 false 로 해야 하는 이유
몇 달 전, 다른 회사에서 개발된 프로젝트를 인수받아 SM 업무를 맡게 되었습니다.
해당 프로젝트는 Spring JPA를 사용하고 있었는데, 테스트 과정에서 조회 부분에서 불필요하게 과도한 데이터가 조회되고 있는 현상이 발견되었습니다.
캠페인 정보를 요청했는데, 캠페인에 속한 청약 및 청약의 타겟 정보들까지 추가로 조회되어 **N+1 문제**가 발생했습니다.
이는 응답 처리 중 쿼리가 추가적으로 실행된 결과였습니다.

<br/>

## JPA 의 영속성 컨텍스트와 open-in-view
### 영속성 컨텍스트란?
**영속성 컨텍스트(Persistence Context)** 는 엔티티 객체를 저장하고 관리하는 환경입니다.
주요 역할 중 하나로 엔티티의 생명주기를 관리하며 **open-in-view** 의 설정 값에 따라 컨텍스트가 열려있는 범위가 달라집니다.

| 설정                   | 영속성 컨텍스트 유지 범위                          | Lazy Loading | 트랜잭션 종료 후 영속성 컨텍스트 상태         |
|-----------------------|----------------------------------------------|--------------|--------------------------------------|
| open-in-view=true     | HTTP 요청 시작부터 끝까지 (View 포함)              | 가능          | 유지됨                                 |
| open-in-view=false    | Service 계층 내에서 트랜잭션 범위 내로 제한          | 불가능         | 종료됨                                 |

open-in-view 의 기본 값은 true 이며 다음과 같이 설정할 수 있습니다.
```yaml
spring:
  jpa:
    open-in-view: false # 또는 true
```

## 그래서 N+1 문제가 발생한 이유가 무엇인가?
```kotlin
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

@Service
class CampaignService(val campaignRepository: CampaignRepository) {

    fun getAllCampaigns(): List<Campaign> {
        return campaignRepository.findAll()
    }
}

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
```

코드를 살펴보면 단순히 `List<Campaign>` 를 반환하고 연관된 엔티티에는 접근하지 않는 것처럼 보입니다.
하지만 실제로는 **HTTP 응답 처리 과정**에서 `HttpMessageConverter`가 반환 데이터를 직렬화 하면서 엔티티의 모든 필드에 접근하기 때문에 문제가 발생합니다.
```json
[
  {
    "id": 1,
    "name": "캠페인 0",
    "contracts": [
      {
        "id": 1,
        "title": "캠페인 0 - 청약 0",
        "targets": [
          {
            "id": 1,
            "target": {
              "id": 6,
              "name": "부천"
            }
          },
          {
            "id": 2,
            "target": {
              "id": 7,
              "name": "세종"
            }
          }
          ...
        ]
      },
    ...
  },
  ...
],
```

## open-in-view=false 만으로 충분하지 않다
`open-in-view`를 `false`로 설정하면 요청 범위에서 영속성 컨텍스트가 닫히기 때문에 Lazy Loading 문제를 방지할 수 있습니다. 그러나, 이 방법만으로는 충분하지 않습니다.
![img.png](img/httpMessageNotWritableException.png)
영속성 컨텍스트 유지 범위가 Service 계층 내에서 트랜잭션 범위 내로 제한되었지만 HttpMessageConverter 가 반환 데이터를 직렬화 하는 과정에 
연관 엔티티에 접근하는 것은 변함 없습니다. 하지만 영속성 컨텍스트는 이미 닫힌 상태이기 때문에 **`LazyInitializationException`** 오류가 발생합니다.
결국 이를 해결하기 위해서는 직렬화 과정에서 연관 엔티티에 접근하는 것을 막아야 된다는 것을 의미하는데 이미 존재하고 있는 필드를 어떻게 접근하지 않도록 할 수 있을까요?
답은 엔티티를 엔티티가 아닌 타입으로 변환하는 것입니다.
```kotlin
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
```
Service 레이어에서 엔티티가 아닌 DTO 로 변환하여 반환함으로서 HttpMessageConverter 가 직렬화하는 시점에 연관 엔티티에 접근하는 상황이 발생하지 않게 되었습니다.

## 마치며
`open-in-view` 를 `false` 로 변경하면서 백엔드 외의 프론트 부분까지 많은 부분을 수정해야되서 팀원 모두가 고생했던 경험이었습니다.
개발 초기에 지연로딩으로 연관 데이터까지 조회되는 편리함을 활용했던 것으로 보이지만 프로젝트의 규모가 커지는 상황에서 그대로 방치할 수 없는 문제였습니다.
나중에 JPA 를 활용해 신규 개발에 참여하게 되는 상황을 마주한다면 아마 `open-in-view` 는 `false` 로 설정하고 시작하게 되지 않을까 싶습니다.
