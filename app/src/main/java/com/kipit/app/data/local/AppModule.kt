package com.kipit.app.data.local
import android.content.Context
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Singleton
import com.kipit.app.domain.repository.KipitRepository
import com.kipit.app.data.repository.KipitRepositoryImpl
@Module @InstallIn(SingletonComponent::class) object AppModule {
 @Provides @Singleton fun database(@ApplicationContext context:Context)=Room.databaseBuilder(context,KipitDatabase::class.java,"kipit.db").setJournalMode(androidx.room.RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING).apply { if(com.kipit.app.BuildConfig.DEMO_DATA) addCallback(SampleSeed()) }.build()
 @Provides fun dao(db:KipitDatabase)=db.dao()
 @Provides fun repository(impl:KipitRepositoryImpl):KipitRepository=impl
}
