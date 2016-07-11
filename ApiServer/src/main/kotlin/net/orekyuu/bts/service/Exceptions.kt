package net.orekyuu.bts.service

import net.orekyuu.bts.domain.AppUser
import net.orekyuu.bts.domain.Team
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.client.HttpStatusCodeException

//HttpStatus系
@ResponseStatus(HttpStatus.NOT_FOUND)
abstract class NotFoundException(statusText: String) : HttpStatusCodeException(HttpStatus.NOT_FOUND, statusText)

@ResponseStatus(HttpStatus.FORBIDDEN)
abstract class ForbiddenException(statusText: String) : HttpStatusCodeException(HttpStatus.FORBIDDEN, statusText)

//Team関係
/**
 * Teamが見つからない時に投げられる
 */
class TeamNotFoundException(id: String) : NotFoundException("specified team[id=$id] does not exist")

/**
 * 指定したuserがTeamに参加していない時に投げられる
 */
class NotJoinTeamMemberException(appUser: AppUser, team: Team) : NotFoundException("${appUser.userName} does not join team[id=${team.id},name=${team.teamName}]")

/**
 * Teamへのアクセス権が必要なときに投げられる
 */
class TeamAccessAuthorityNotException(appUser: AppUser, team: Team) :
        ForbiddenException("user[id=${appUser.id},name=${appUser.userName}] is no access authority to the specified team[id=${team.id},name=${team.teamName}]")

//Product関係
class ProductNotFoundException(team: Team, productId: Int) :
        NotFoundException("is not registered product[id=$productId] in this team[id=${team.id},name=${team.teamName}]")