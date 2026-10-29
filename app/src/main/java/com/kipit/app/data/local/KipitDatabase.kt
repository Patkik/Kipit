package com.kipit.app.data.local
import androidx.room.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.data.local.dao.KipitDao
@Database(entities=[WalletEntity::class,CategoryEntity::class,TransactionEntity::class,SplitReceivableEntity::class,ImpulseVaultEntity::class,UserConfigEntity::class,TagFts::class,PendingCaptureEntity::class],version=1,exportSchema=true)
@TypeConverters(Converters::class)
abstract class KipitDatabase:RoomDatabase(){abstract fun dao():KipitDao}
