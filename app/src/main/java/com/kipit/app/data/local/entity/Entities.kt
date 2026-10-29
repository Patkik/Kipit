package com.kipit.app.data.local.entity
import androidx.room.*
import com.kipit.app.core.model.*
@Entity(tableName="wallets") data class WalletEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val name:String,val type:WalletType,val balanceCentavos:Long,val isArchived:Boolean=false)
@Entity(tableName="categories") data class CategoryEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val name:String,val iconName:String="circle",val colorHex:Long=0xFF426B5B,val monthlyLimitCentavos:Long,val isDiscretionary:Boolean=true,val isFixedBill:Boolean=false)
@Entity(tableName="transactions",indices=[Index(value=["timestampEpochMs","type"]),Index(value=["categoryId","timestampEpochMs"]),Index(value=["hourOfDay","isWeekend"]),Index(value=["operationKey"],unique=true)]) data class TransactionEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val type:TxType,val amountCentavos:Long,val feeCentavos:Long=0,val sourceWalletId:Long,val destinationWalletId:Long?=null,val categoryId:Long?=null,val timestampEpochMs:Long,val hourOfDay:Int,val dayOfWeek:Int,val isWeekend:Boolean,val note:String="",val tagsCsv:String="",val operationKey:String=java.util.UUID.randomUUID().toString())
@Entity(tableName="split_receivables") data class SplitReceivableEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val parentTransactionId:Long,val debtorName:String,val amountOwedCentavos:Long,val isSettled:Boolean=false,val createdAtMs:Long,val settledAtMs:Long?=null)
@Entity(tableName="impulse_vault") data class ImpulseVaultEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val itemName:String,val amountCentavos:Long,val categoryId:Long,val createdAtMs:Long,val unlocksAtMs:Long=createdAtMs+604800000L,val status:ImpulseStatus=ImpulseStatus.COOLING_OFF)
@Entity(tableName="user_config") data class UserConfigEntity(@PrimaryKey val id:Int=1,val mealAnchorCentavos:Long=7500,val savingsGoalCentavos:Long=1000000,val lockedSavingsCentavos:Long=0,val currentNoSpendStreak:Int=0,val impulseSavedCentavos:Long=0,val lastSweepDate:String="",val lastStreakDate:String="")
@Entity(tableName="pending_captures",indices=[Index(value=["sourceKey"],unique=true)]) data class PendingCaptureEntity(@PrimaryKey(autoGenerate=true)val id:Long=0,val sourceKey:String,val packageName:String,val amountCentavos:Long,val feeCentavos:Long=0,val merchant:String,val createdAtMs:Long)
@Fts4 @Entity(tableName="tag_fts") data class TagFts(@PrimaryKey @ColumnInfo(name="rowid")val rowid:Int,val note:String,val tagsCsv:String)
class Converters {
 @TypeConverter fun money(v:Centavos):Long=v.value
 @TypeConverter fun money(v:Long):Centavos=Centavos(v)
 @TypeConverter fun wallet(v:WalletType):String=v.name
 @TypeConverter fun wallet(v:String):WalletType=WalletType.valueOf(v)
 @TypeConverter fun tx(v:TxType):String=v.name
 @TypeConverter fun tx(v:String):TxType=TxType.valueOf(v)
 @TypeConverter fun impulse(v:ImpulseStatus):String=v.name
 @TypeConverter fun impulse(v:String):ImpulseStatus=ImpulseStatus.valueOf(v)
}
