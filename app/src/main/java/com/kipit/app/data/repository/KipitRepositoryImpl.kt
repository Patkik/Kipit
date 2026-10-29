package com.kipit.app.data.repository
import androidx.room.withTransaction
import com.kipit.app.core.model.*
import com.kipit.app.data.local.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.domain.repository.KipitRepository
import javax.inject.Inject
import javax.inject.Singleton
import java.time.*
import kotlinx.coroutines.flow.first
@Singleton class KipitRepositoryImpl @Inject constructor(val db:KipitDatabase):KipitRepository {
 val dao=db.dao()
 override val wallets=dao.wallets(); override val categories=dao.categories(); override val transactions=dao.transactions()
 override suspend fun commit(tx:TransactionEntity,owed:Long,debtor:String)=db.withTransaction {
 require(tx.amountCentavos>0 && tx.feeCentavos>=0 && owed>=0){"Enter a positive amount"}
 require(dao.operationExists(tx.operationKey)==0){"Already recorded"}
 val source=dao.wallet(tx.sourceWalletId)?:error("Wallet unavailable")
 require(!source.isArchived){"Wallet is archived"}
 require(source.type!=WalletType.CREDIT){"Credit account transactions are not supported yet"}
 require(tx.type==TxType.EXPENSE || owed==0L){"Only expenses can have a receivable"}
 require(tx.type !in listOf(TxType.INCOME,TxType.OPENING) || tx.feeCentavos==0L){"Income cannot include an expense fee"}
 require(tx.type==TxType.TRANSFER || tx.destinationWalletId==null){"Only transfers can have a destination"}
 if(tx.type==TxType.EXPENSE) require(dao.categories().first().any{it.id==tx.categoryId}){"Choose a valid category"}
 else require(tx.categoryId==null){"Only expenses can have a category"}
 if(tx.type==TxType.OPENING)require(dao.transactions().first().none{it.sourceWalletId==source.id || it.destinationWalletId==source.id} && source.balanceCentavos==0L){"Opening balance requires an unused wallet"}
 val total=Math.addExact(Math.addExact(tx.amountCentavos,tx.feeCentavos),owed)
 if(tx.type !in listOf(TxType.INCOME,TxType.OPENING))require(source.type==WalletType.CREDIT || source.balanceCentavos>=total){"This wallet has insufficient funds"}
 val change=if(tx.type in listOf(TxType.INCOME,TxType.OPENING))tx.amountCentavos else -total
 dao.updateWallet(source.copy(balanceCentavos=Math.addExact(source.balanceCentavos,change)))
 if(tx.type==TxType.TRANSFER){require(owed==0L && tx.destinationWalletId!=source.id);val destination=dao.wallet(tx.destinationWalletId?:error("Choose destination"))?:error("Wallet unavailable");require(!destination.isArchived && destination.type!=WalletType.CREDIT){"Choose an active cash, bank or e-wallet destination"};dao.updateWallet(destination.copy(balanceCentavos=Math.addExact(destination.balanceCentavos,tx.amountCentavos)))}
 val id=dao.insertTx(tx);dao.index(TagFts(Math.toIntExact(id),tx.note,tx.tagsCsv))
 if(owed>0){require(debtor.isNotBlank()){ "Enter a debtor name" };dao.insertReceivable(SplitReceivableEntity(parentTransactionId=id,debtorName=debtor,amountOwedCentavos=owed,createdAtMs=tx.timestampEpochMs))}
 id
 }
 override suspend fun settle(id:Long)=db.withTransaction {
 val loan=dao.receivable(id)?:error("Receivable unavailable");require(!loan.isSettled){"Already settled"}
 val parent=dao.transaction(loan.parentTransactionId)?:error("Original transaction unavailable");val wallet=dao.wallet(parent.sourceWalletId)?:error("Wallet unavailable")
 dao.updateWallet(wallet.copy(balanceCentavos=Math.addExact(wallet.balanceCentavos,loan.amountOwedCentavos)));dao.updateReceivable(loan.copy(isSettled=true,settledAtMs=System.currentTimeMillis()))
 }
 override suspend fun decideImpulse(id:Long,buy:Boolean,walletId:Long)=db.withTransaction {
 val item=dao.impulse(id)?:error("Item unavailable");require(item.status==ImpulseStatus.COOLING_OFF && System.currentTimeMillis()>=item.unlocksAtMs){"Cooling-off period is still active"}
 if(buy){val now=ZonedDateTime.now();commit(TransactionEntity(type=TxType.EXPENSE,amountCentavos=item.amountCentavos,sourceWalletId=walletId,categoryId=item.categoryId,timestampEpochMs=now.toInstant().toEpochMilli(),hourOfDay=now.hour,dayOfWeek=now.dayOfWeek.value,isWeekend=now.dayOfWeek.value>=6,note=item.itemName))}
 else {val config=dao.readConfig()?:error("Configuration unavailable");val date=LocalDate.now();val safe=com.kipit.app.domain.usecase.CalculateSafeToSpendUseCase()(dao.walletsSnapshot(),dao.categories().first(),dao.transactions().first(),config,date);require(Math.multiplyExact(safe.value,(date.lengthOfMonth()-date.dayOfMonth+1).toLong())>=item.amountCentavos){"Not enough unreserved cash to lock this saving"};dao.setConfig(config.copy(lockedSavingsCentavos=Math.addExact(config.lockedSavingsCentavos,item.amountCentavos),impulseSavedCentavos=Math.addExact(config.impulseSavedCentavos,item.amountCentavos)))}
 dao.updateImpulse(item.copy(status=if(buy)ImpulseStatus.PURCHASED else ImpulseStatus.SKIPPED_AND_SAVED))
 }
}
