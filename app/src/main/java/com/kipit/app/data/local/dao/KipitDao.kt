package com.kipit.app.data.local.dao
import androidx.room.*
import kotlinx.coroutines.flow.Flow
import com.kipit.app.data.local.entity.*
data class HabitMacro(val amountCentavos:Long,val categoryId:Long?,val sourceWalletId:Long,val note:String,val freq:Int)
data class TriggerWindow(val dayOfWeek:Int,val bucket:Int,val total:Long,val count:Int)
data class MicroLeak(val categoryId:Long?,val note:String,val count:Int,val total:Long)
@Dao interface KipitDao {
 @Query("SELECT * FROM wallets WHERE isArchived=0")suspend fun walletsSnapshot():List<WalletEntity>
 @Query("SELECT * FROM wallets WHERE isArchived=0 ORDER BY id")fun wallets():Flow<List<WalletEntity>>
 @Query("SELECT * FROM categories ORDER BY id")fun categories():Flow<List<CategoryEntity>>
 @Query("SELECT * FROM transactions ORDER BY timestampEpochMs DESC")fun transactions():Flow<List<TransactionEntity>>
 @Query("SELECT * FROM user_config WHERE id=1")fun config():Flow<UserConfigEntity?>
 @Query("SELECT * FROM split_receivables WHERE isSettled=0")fun receivables():Flow<List<SplitReceivableEntity>>
 @Query("SELECT * FROM impulse_vault WHERE status='COOLING_OFF'")fun impulses():Flow<List<ImpulseVaultEntity>>
 @Query("SELECT * FROM pending_captures ORDER BY createdAtMs DESC")fun captures():Flow<List<PendingCaptureEntity>>
 @Query("SELECT * FROM wallets WHERE id=:id")suspend fun wallet(id:Long):WalletEntity?
 @Query("SELECT * FROM user_config WHERE id=1")suspend fun readConfig():UserConfigEntity?
 @Query("SELECT * FROM split_receivables WHERE id=:id")suspend fun receivable(id:Long):SplitReceivableEntity?
 @Query("SELECT * FROM transactions WHERE id=:id")suspend fun transaction(id:Long):TransactionEntity?
 @Query("SELECT * FROM impulse_vault WHERE id=:id")suspend fun impulse(id:Long):ImpulseVaultEntity?
 @Query("SELECT COUNT(*) FROM transactions WHERE operationKey=:key")suspend fun operationExists(key:String):Int
 @Insert suspend fun insertWallet(v:WalletEntity):Long
 @Insert suspend fun insertCategory(v:CategoryEntity):Long
 @Insert suspend fun insertTx(v:TransactionEntity):Long
 @Insert suspend fun insertReceivable(v:SplitReceivableEntity):Long
 @Insert suspend fun insertImpulse(v:ImpulseVaultEntity):Long
 @Insert(onConflict=OnConflictStrategy.IGNORE)suspend fun insertCapture(v:PendingCaptureEntity):Long
 @Insert(onConflict=OnConflictStrategy.REPLACE)suspend fun setConfig(v:UserConfigEntity)
 @Insert suspend fun index(v:TagFts)
 @Update suspend fun updateCategory(v:CategoryEntity)
 @Update suspend fun updateWallet(v:WalletEntity)
 @Update suspend fun updateReceivable(v:SplitReceivableEntity)
 @Update suspend fun updateImpulse(v:ImpulseVaultEntity)
 @Query("DELETE FROM pending_captures WHERE id=:id")suspend fun deleteCapture(id:Long)
 @Query("SELECT * FROM transactions WHERE id IN (SELECT rowid FROM tag_fts WHERE tag_fts MATCH :query) ORDER BY timestampEpochMs DESC")suspend fun search(query:String):List<TransactionEntity>
 @Query("""SELECT categoryId FROM transactions WHERE timestampEpochMs>=:since AND type='EXPENSE' AND categoryId IS NOT NULL GROUP BY categoryId ORDER BY SUM(CASE WHEN MIN(ABS(hourOfDay-:hour),24-ABS(hourOfDay-:hour))<=1 AND isWeekend=:weekend THEN 3.0 WHEN MIN(ABS(hourOfDay-:hour),24-ABS(hourOfDay-:hour))<=2 THEN 2.0 ELSE 1.0 END) DESC LIMIT 6""")suspend fun predictions(since:Long,hour:Int,weekend:Boolean):List<Long>
 @Query("""SELECT amountCentavos,categoryId,sourceWalletId,note,COUNT(*) as freq FROM transactions WHERE timestampEpochMs>=:since AND type='EXPENSE' GROUP BY amountCentavos,categoryId,sourceWalletId,note HAVING freq>=2 ORDER BY freq DESC LIMIT 5""")suspend fun macros(since:Long):List<HabitMacro>
 @Query("""SELECT t.dayOfWeek,t.hourOfDay/3 as bucket,SUM(t.amountCentavos+t.feeCentavos) as total,COUNT(*) as count FROM transactions t JOIN categories c ON c.id=t.categoryId WHERE c.isDiscretionary=1 AND t.type='EXPENSE' AND t.timestampEpochMs>=:since GROUP BY t.dayOfWeek,t.hourOfDay/3 ORDER BY total DESC""")suspend fun triggers(since:Long):List<TriggerWindow>
 @Query("""SELECT categoryId,note,COUNT(*) as count,SUM(amountCentavos+feeCentavos) as total FROM transactions WHERE type='EXPENSE' AND amountCentavos<=15000 AND timestampEpochMs>=:since GROUP BY categoryId,note HAVING count>=2 ORDER BY total DESC""")suspend fun leaks(since:Long):List<MicroLeak>
}
