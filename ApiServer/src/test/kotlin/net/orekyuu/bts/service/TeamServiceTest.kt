package net.orekyuu.bts.service

import net.orekyuu.bts.ApiServerApplication
import net.orekyuu.bts.config.BTSResourceServerConfigurer
import net.orekyuu.bts.config.BtsApplicationConfig
import net.orekyuu.bts.domain.AppUser
import net.orekyuu.bts.message.team.TeamInfo
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.transactions.transaction
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.SpringApplicationConfiguration
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner
import org.springframework.test.context.web.WebAppConfiguration

import org.assertj.core.api.Assertions.*
import kotlin.system.measureNanoTime

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(ApiServerApplication::class, BtsApplicationConfig::class, BTSResourceServerConfigurer::class))
@WebAppConfiguration
class TeamServiceTest {

    @Autowired
    lateinit var teamService: TeamService
    @Autowired
    lateinit var userService: UserService

    lateinit var user1: AppUser
    lateinit var user2: AppUser
    lateinit var user3: AppUser

    lateinit var teamInfo: TeamInfo
    @Before
    fun setUp() {
        transaction {
            SchemaUtils.create(*BtsApplicationConfig.TABLE_LIST)
        }

        user1 = userService.createAppUserFromGithub("user1")
        user2 = userService.createAppUserFromGithub("user2")
        user3 = userService.createAppUserFromGithub("user3")

        teamInfo = teamService.createTeam(user1, "teamInfo", "name")
    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(*BtsApplicationConfig.TABLE_LIST)
        }
    }

    @Test
    fun createTeam() {
        val result = teamService.createTeam(user1, "team1_id", "team1")
        assertThat(result.teamId).isEqualTo("team1_id")
        assertThat(result.teamName).isEqualTo("team1")
        val time = measureNanoTime {
            assertThat(result.member.count()).isEqualTo(1)//0.009msとか
        }
        println("${time * 0.000001}ms")
        assertThat(result.product.count()).isEqualTo(0)
        assertThat(result.owner.id).isEqualTo(user1.id.value)
    }

    @Test
    fun joinTeam() {
        val teamId = "joinTeamTest"
        teamService.createTeam(user1, teamId, "team")
        var result: TeamInfo? = null
        val time = measureNanoTime {
            result = teamService.joinTeam(user1, teamId, user2)//10~20ms
        }
        println("${time * 0.000001}ms")
        val time2 = measureNanoTime {
            assertThat(result!!.member.count()).isEqualTo(2)//なぜかこいつは70~100msくらいかかってる...
        }
        println("${time2 * 0.000001}ms")
    }

    @Test(expected = TeamNotFoundException::class)
    fun joinTeamThrownTeamThrownNotFoundException() {
        teamService.joinTeam(user2, "hogehoge", user2)
    }

    @Test(expected = TeamAccessAuthorityNotException::class)
    fun joinTeamThrownTeamAccessAuthorityNotException() {
        teamService.joinTeam(user3, teamInfo.teamId, user3)
    }

    @Test
    fun defectionTeam() {
        val teamId = "defectionTeamTest"
        teamService.createTeam(user1, teamId, "team")
        teamService.joinTeam(user1, teamId, user2)
        val result = teamService.joinTeam(user1, teamId, user3)
        val time = measureNanoTime {
            assertThat(result.member.count()).isEqualTo(3)//0.01msとか
        }
        println("${time * 0.000001} ms ")
        val result2 = teamService.defectionTeam(user1, teamId, user3)
        val time2 = measureNanoTime {
            assertThat(result2.member.count()).isEqualTo(2)//0.01msとか
        }
        println("${time2 * 0.000001} ms ")
    }

    @Test(expected = NotJoinTeamMemberException::class)
    fun defectionTeamThrownNotJoinTeamMemberException() {
        teamService.defectionTeam(user1, teamInfo.teamId, user3)
    }

    @Test(expected = TeamNotFoundException::class)
    fun defectionTeamThrownTeamNotFoundException() {
        teamService.defectionTeam(user1, "hogehoge", user3)
    }

    @Test(expected = IllegalOperationException::class)
    fun defectionTeamThrownIllegalOperationException() {
        teamService.defectionTeam(user1, teamInfo.teamId, user1)
    }

    @Test
    fun showTeam() {
        val teamId = "showTeamTest"
        val name = "name"
        teamService.createTeam(user1, teamId, name)
        val result = teamService.showTeamInfo(teamId, user1)
        assertThat(result.teamName).isEqualTo(name)
        assertThat(result.member.count()).isEqualTo(1)
        assertThat(result.product.count()).isEqualTo(0)


    }

    @Test(expected = TeamNotFoundException::class)
    fun showTeamInfoThrownTeamNotFoundException() {
        teamService.showTeamInfo("hogehoge", user1)
    }

    @Test(expected = TeamAccessAuthorityNotException::class)
    fun showTeamInfoThrownTeamAccessAuthorityNotException() {
        teamService.showTeamInfo(teamInfo.teamId, user2)
    }

    @Test
    fun showTeamMember() {
        val teamId = "showTeamMember"
        teamService.createTeam(user1, teamId, "team")
        val result = teamService.showTeamMember(teamId, user1)
        assertThat(result.count()).isEqualTo(1)
        teamService.joinTeam(user1, teamId, user2)
        val result2 = teamService.showTeamMember(teamId, user1)
        assertThat(result2.count()).isEqualTo(2)
    }

    @Test(expected = TeamNotFoundException::class)
    fun showTeamMemberThrownTeamNotFoundException() {
        teamService.showTeamMember("hogehoge", user1)
    }

    @Test(expected = TeamAccessAuthorityNotException::class)
    fun showTeamMemberThrownTeamAccessAuthorityNotException() {
        teamService.showTeamMember(teamInfo.teamId, user3)
    }
}
