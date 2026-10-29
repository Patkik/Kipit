package com.kipit.app.domain.repository
import com.kipit.app.data.local.entity.*
import kotlinx.coroutines.flow.Flow
interface KipitRepository {
 val wallets:Flow<List<WalletEntity>>
 val categories:Flow<List<CategoryEntity>>
 val transactions:Flow<List<TransactionEntity>>
 suspend fun commit(tx:TransactionEntity,owed:Long=0,debtor:String=""):Long
 suspend fun settle(id:Long)
 suspend fun decideImpulse(id:Long,buy:Boolean,walletId:Long)
}
