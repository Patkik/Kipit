package com.kipit.app.service
import android.app.*
import android.content.*
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.service.quicksettings.TileService
import androidx.core.app.NotificationCompat
import androidx.work.*
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.layout.*
import androidx.glance.text.*
import androidx.compose.ui.unit.dp
import com.kipit.app.*
import com.kipit.app.core.model.*
import com.kipit.app.data.local.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.domain.usecase.*
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.*
import java.util.concurrent.TimeUnit
@EntryPoint @InstallIn(SingletonComponent::class) interface DatabaseEntry {fun database():KipitDatabase}
fun database(context:Context)=EntryPointAccessors.fromApplication(context.applicationContext,DatabaseEntry::class.java).database()
object LocalNotifications {
 fun show(context:Context,id:Int,title:String,text:String){
  val manager=context.getSystemService(NotificationManager::class.java)
  manager.createNotificationChannel(NotificationChannel("kipit.reminders","Kipit reminders",NotificationManager.IMPORTANCE_DEFAULT))
  if(Build.VERSION.SDK_INT>=33 && context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)return
  val intent=PendingIntent.getActivity(context,id,Intent(context,MainActivity::class.java),PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
  manager.notify(id,NotificationCompat.Builder(context,"kipit.reminders").setSmallIcon(com.kipit.app.R.drawable.ic_kipit).setContentTitle(title).setContentText(text).setStyle(NotificationCompat.BigTextStyle().bigText(text)).setContentIntent(intent).setAutoCancel(true).setVisibility(NotificationCompat.VISIBILITY_PRIVATE).addAction(0,"Review in Kipit",intent).build())
 }
}
data class CapturedPayment(val amount:Long,val fee:Long,val merchant:String)
object PaymentParser {
 val packages=setOf("com.globe.gcash.android","com.paymaya","com.bdo.retail","com.bpi.ng.app","com.grabtaxi.passenger","com.global.foodpanda.android")
 private val amount=Regex("""(?:PHP|₱|P)\s*([0-9][0-9,]*\.[0-9]{2})""",RegexOption.IGNORE_CASE)
 private val fee=Regex("""fee[^0-9]*(?:PHP|₱|P)?\s*([0-9][0-9,]*\.[0-9]{2})""",RegexOption.IGNORE_CASE)
 private val outgoing=Regex("""\b(paid|sent|payment successful|purchase|debited)\b""",RegexOption.IGNORE_CASE)
 private val incoming=Regex("""\b(received|credited|refund|reversed|failed|declined|OTP|verification code)\b""",RegexOption.IGNORE_CASE)
 fun parse(pkg:String,text:String):CapturedPayment? {
 if(pkg !in packages || !outgoing.containsMatchIn(text) || incoming.containsMatchIn(text))return null
 return runCatching{val m=amount.find(text)?:return null;val n=Centavos.parse(m.groupValues[1]).value;require(n>0);CapturedPayment(n,fee.find(text)?.let{Centavos.parse(it.groupValues[1]).value}?:0,"Captured payment")}.getOrNull()
 }
}
class EWalletNotificationListener:NotificationListenerService(){
 private val scope=CoroutineScope(SupervisorJob()+Dispatchers.IO)
 override fun onNotificationPosted(sbn:StatusBarNotification){
 if(sbn.packageName !in PaymentParser.packages)return
 val extras=sbn.notification.extras;val text=listOf(extras.getCharSequence(Notification.EXTRA_TITLE),extras.getCharSequence(Notification.EXTRA_TEXT),extras.getCharSequence(Notification.EXTRA_BIG_TEXT)).filterNotNull().joinToString(" ")
 val payment=PaymentParser.parse(sbn.packageName,text)?:return
 scope.launch{val id=database(this@EWalletNotificationListener).dao().insertCapture(PendingCaptureEntity(sourceKey=sbn.key+":"+sbn.postTime,packageName=sbn.packageName,amountCentavos=payment.amount,feeCentavos=payment.fee,merchant=payment.merchant,createdAtMs=sbn.postTime));if(id>0)LocalNotifications.show(this@EWalletNotificationListener,Math.floorMod(id,Int.MAX_VALUE.toLong()).toInt(),"Payment ready to review","A payment was captured locally. Open Kipit to confirm the wallet and category.")}
 }
 override fun onDestroy(){scope.cancel();super.onDestroy()}
}
class SurplusSweepWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result=try{
 val db=database(applicationContext);val dao=db.dao();val config=dao.readConfig()
 if(config!=null){val safe=CalculateSafeToSpendUseCase()(dao.wallets().first(),dao.categories().first(),dao.transactions().first(),config);if(safe.value>0)LocalNotifications.show(applicationContext,900,"A little left over?","Your current daily room is ${safe.toPhpString()}. Open Vault to review today's savings sweep.")}
 Schedules.evening(applicationContext,ExistingWorkPolicy.APPEND_OR_REPLACE);Result.success()
 }catch(e:Exception){if(e is CancellationException)throw e;Result.retry()}}
class MidnightRecalibrationWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result=try{KipitGlanceWidget().updateAll(applicationContext);Schedules.midnight(applicationContext,ExistingWorkPolicy.APPEND_OR_REPLACE);Result.success()}catch(e:Exception){if(e is CancellationException)throw e;Result.retry()}}
class WeeklyDiagnosticsWorker(context:Context,params:WorkerParameters):CoroutineWorker(context,params){override suspend fun doWork():Result=try{val d=RunBehavioralDiagnosticsUseCase(database(applicationContext).dao())(System.currentTimeMillis());if(d.leaks.isNotEmpty())LocalNotifications.show(applicationContext,901,"Your weekly money check-in","You have ${d.leaks.size} recurring small spending patterns to explore in Deep Lab.");Result.success()}catch(e:Exception){if(e is CancellationException)throw e;Result.retry()}}
object Schedules {
 fun install(context:Context){evening(context);midnight(context);WorkManager.getInstance(context).enqueueUniquePeriodicWork("kipit.weekly",ExistingPeriodicWorkPolicy.KEEP,PeriodicWorkRequestBuilder<WeeklyDiagnosticsWorker>(7,TimeUnit.DAYS).build())}
 private fun delay(hour:Int):Long{val now=ZonedDateTime.now();var next=now.toLocalDate().atTime(hour,0).atZone(now.zone);if(!next.isAfter(now))next=next.plusDays(1);return Duration.between(now,next).toMillis()}
 fun evening(context:Context,policy:ExistingWorkPolicy=ExistingWorkPolicy.KEEP){WorkManager.getInstance(context).enqueueUniqueWork("kipit.evening",policy,OneTimeWorkRequestBuilder<SurplusSweepWorker>().setInitialDelay(delay(21),TimeUnit.MILLISECONDS).build())}
 fun midnight(context:Context,policy:ExistingWorkPolicy=ExistingWorkPolicy.KEEP){WorkManager.getInstance(context).enqueueUniqueWork("kipit.midnight",policy,OneTimeWorkRequestBuilder<MidnightRecalibrationWorker>().setInitialDelay(delay(0),TimeUnit.MILLISECONDS).build())}
}
class QuickMacroTileService:TileService(){override fun onClick(){super.onClick();val intent=Intent(this,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);val pending=PendingIntent.getActivity(this,300,intent,PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT);if(Build.VERSION.SDK_INT>=34)startActivityAndCollapse(pending)else pending.send()}}
class KipitGlanceWidget:GlanceAppWidget(){override suspend fun provideGlance(context:Context,id:GlanceId){val dao=database(context).dao();val config=dao.readConfig();val safe=if(config!=null)CalculateSafeToSpendUseCase()(dao.wallets().first(),dao.categories().first(),dao.transactions().first(),config) else Centavos(0);provideContent{Column(GlanceModifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFE5EFE8)).padding(16.dp)){Text("Kipit · Safe today");Text(safe.toPhpString());Button("Open Kipit",onClick=actionStartActivity(Intent(context,MainActivity::class.java)))}}}}
class KipitWidgetReceiver:GlanceAppWidgetReceiver(){override val glanceAppWidget:GlanceAppWidget=KipitGlanceWidget()}
