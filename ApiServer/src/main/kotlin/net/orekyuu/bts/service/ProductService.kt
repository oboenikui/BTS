package net.orekyuu.bts.service

import net.orekyuu.bts.domain.AppUser
import net.orekyuu.bts.domain.Product
import net.orekyuu.bts.domain.ProductTable
import net.orekyuu.bts.domain.Team
import net.orekyuu.bts.message.product.ProductInfo
import net.orekyuu.bts.message.team.TeamInfo
import org.jetbrains.exposed.sql.transactions.transaction
import java.util.*

interface ProductService {

    /**
     * プロダクトをチームに登録
     */
    fun registerToTeam(requestUser: AppUser, teamId: String, productName: String): TeamInfo

    /**
     * プロダクトをチームから削除
     */
    fun deleteFromTeam(requestUser: AppUser, teamId: String, productId: Int): TeamInfo

    /**
     * チームからプロダクトの一覧を取得
     */
    fun showProductsFromTeam(requestUser: AppUser, teamId: String): List<ProductInfo>

    /**
     *  プロダクトの名前を変更
     */
    fun modifyProductName(requestUser: AppUser, teamId: String, productId: Int, productName: String): ProductInfo

    /**
     * プロダクトトークンの再生成を行う
     */
    fun regenerateProductToken(requestUser: AppUser, teamId: String, productId: Int): ProductInfo
}

class ProductServiceImpl : ProductService {

    override fun registerToTeam(requestUser: AppUser, teamId: String, productName: String): TeamInfo = transaction {
        val team = Team.findById(teamId) ?: throw TeamNotFoundException(teamId)
        checkAuthority(team, requestUser)
        val product = Product.new {
            this.productName = productName
            this.team = team
        }
        ofTeamInfo(product.team)
    }

    override fun deleteFromTeam(requestUser: AppUser, teamId: String, productId: Int): TeamInfo = transaction {
        val team = Team.findById(teamId) ?: throw TeamNotFoundException(teamId)
        checkAuthority(team, requestUser)
        val product = Product.findById(productId) ?: throw ProductNotFoundException(team, productId)
        product.delete()
        ofTeamInfo(team)
    }

    override fun showProductsFromTeam(requestUser: AppUser, teamId: String): List<ProductInfo> = transaction {
        val team = Team.findById(teamId) ?: throw TeamNotFoundException(teamId)
        checkAuthority(team, requestUser)
        val products = Product.find { ProductTable.team.eq(team.id) }
        products.asSequence().map { ofProductInfo(it) }.toList()
    }

    override fun modifyProductName(requestUser: AppUser, teamId: String,
                                   productId: Int, productName: String): ProductInfo = transaction {
        val team = Team.findById(teamId) ?: throw TeamNotFoundException(teamId)
        checkAuthority(team, requestUser)
        val product = Product.findById(productId) ?: throw ProductNotFoundException(team, productId)
        product.productName = productName
        ofProductInfo(product)
    }

    override fun regenerateProductToken(requestUser: AppUser, teamId: String, productId: Int): ProductInfo = transaction {
        val team = Team.findById(teamId) ?: throw TeamNotFoundException(teamId)
        checkAuthority(team, requestUser)
        val product = Product.findById(productId) ?: throw ProductNotFoundException(team, productId)
        product.productToken = UUID.randomUUID()
        ofProductInfo(product)
    }

    private fun checkAuthority(team: Team, appUser: AppUser) {
        if (!team.member.any { it.id == appUser.id })
            throw TeamAccessAuthorityNotException(appUser, team)
    }
}