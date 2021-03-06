package net.orekyuu.bts.api.user

import com.fasterxml.jackson.databind.ObjectMapper
import net.orekyuu.bts.ApiServerApplication
import net.orekyuu.bts.api.user.util.MockOAuth2Util
import net.orekyuu.bts.config.BTSResourceServerConfigurer
import net.orekyuu.bts.config.BtsApplicationConfig
import net.orekyuu.bts.domain.AppUser
import net.orekyuu.bts.service.TeamService
import net.orekyuu.bts.service.UserService
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
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext

import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.assertj.core.api.Assertions.*
import org.springframework.http.MediaType

@RunWith(SpringJUnit4ClassRunner::class)
@SpringApplicationConfiguration(classes = arrayOf(ApiServerApplication::class, BtsApplicationConfig::class, BTSResourceServerConfigurer::class))
@WebAppConfiguration
class TeamApiControllerTest {

    @Autowired
    lateinit var context: WebApplicationContext
    @Autowired
    lateinit var userService: UserService
    @Autowired
    lateinit var teamService: TeamService
    @Autowired
    lateinit var mockSecurity: MockOAuth2Util

    lateinit var user1: AppUser
    lateinit var user2: AppUser

    lateinit var mock: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper;

    @Before
    fun setUp() {
        transaction {
            SchemaUtils.create(*BtsApplicationConfig.TABLE_LIST)
        }

        user1 = userService.createAppUserFromGithub("user1")
        user2 = userService.createAppUserFromGithub("user2")

        mock = MockMvcBuilders
                .webAppContextSetup(context)
                .build()

    }

    @After
    fun tearDown() {
        transaction {
            SchemaUtils.drop(*BtsApplicationConfig.TABLE_LIST)
        }
    }

    @Test
    fun testShowTeam() {
        mockSecurity.enableGithubMock("user1")

        teamService.createTeam(user1, "test", "test")
        mock.perform(MockMvcRequestBuilders.get("/team/show").param("teamId", "test"))
                .andExpect(status().isOk)

        mock.perform(MockMvcRequestBuilders.get("/team/show").param("teamId", "test2"))
                .andExpect(status().isNotFound)
    }

    @Test
    fun testShowMember() {
        mockSecurity.enableGithubMock("user1")

        teamService.createTeam(user1, "test", "test")

        mock.perform(MockMvcRequestBuilders.get("/team/member/show").param("teamId", "test"))
                .andExpect(status().isOk)
    }

    @Test
    fun createTeam() {
        mockSecurity.enableGithubMock("user1")

        val req = """
        { "teamId": "test", "teamName": "hoge"}
        """

        mock.perform(MockMvcRequestBuilders.post("/team/create").contentType(MediaType.APPLICATION_JSON)
                .content(req)).andExpect(status().isOk)

        val team = teamService.showTeamInfo("test", user1)
        assertThat(team).isNotNull()
        assertThat(team.teamId).isEqualTo("test")
        assertThat(team.teamName).isEqualTo("hoge")
        assertThat(team.member.size).isEqualTo(1)
        assertThat(team.member[0].id).isEqualTo(user1.id.value)
    }

    @Test
    fun joinTeam() {
        mockSecurity.enableGithubMock("user1")

        teamService.createTeam(user1, "team1", "team1")
        val req = """
        { "teamId": "team1", "userId": "${user2.id.value}"}
        """

        mock.perform(MockMvcRequestBuilders.post("/team/member/join").contentType(MediaType.APPLICATION_JSON)
                .content(req)).andExpect(status().isOk)

        val member = teamService.showTeamMember("team1", user1)
        assertThat(member.size).isEqualTo(2)
    }

    @Test
    fun defectionTeam() {
        mockSecurity.enableGithubMock("user1")

        teamService.createTeam(user1, "team1", "team1")
        teamService.joinTeam(user1, "team1", user2)

        val req = """
        { "teamId": "team1", "userId": "${user2.id.value}"}
        """
        mock.perform(MockMvcRequestBuilders.post("/team/member/defection").contentType(MediaType.APPLICATION_JSON)
                .content(req)).andExpect(status().isOk)

        val member = teamService.showTeamMember("team1", user1)
        assertThat(member.size).isEqualTo(1)
        assertThat(member[0].id).isEqualTo(user1.id.value)
    }

}
