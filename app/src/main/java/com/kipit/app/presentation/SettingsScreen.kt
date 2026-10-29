package com.kipit.app.presentation

import android.Manifest
import android.app.NotificationManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.kipit.app.core.model.*
import com.kipit.app.presentation.vault.MoneyEditor
import com.kipit.app.service.UpdateCheckResult

@Composable fun SettingsScreen(s:UiState,vm:AppViewModel) {
    val context=LocalContext.current
    val lifecycle=LocalLifecycleOwner.current
    val updateState by vm.updateState.collectAsState()
    var revision by remember{mutableIntStateOf(0)}
    DisposableEffect(lifecycle){val observer=LifecycleEventObserver{_,event->if(event==Lifecycle.Event.ON_RESUME)revision++};lifecycle.lifecycle.addObserver(observer);onDispose{lifecycle.lifecycle.removeObserver(observer)}}
    val notifications=remember(revision){context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()}
    val captureEnabled=remember(revision){Settings.Secure.getString(context.contentResolver,"enabled_notification_listeners")?.split(':')?.any{it.substringBefore('/')==context.packageName}==true}
    val permission=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()){revision++}
    var editor by rememberSaveable{mutableStateOf("")}
    fun open(value:String){vm.clearError();editor=value}

    val updateSubtitle = when (val u = updateState) {
        is UpdateCheckResult.Checking -> "Checking for updates..."
        is UpdateCheckResult.Available -> "Update available (${u.newVersion})"
        is UpdateCheckResult.UpToDate -> "v${com.kipit.app.BuildConfig.VERSION_NAME} • Up to date"
        is UpdateCheckResult.Error -> "Check failed"
        UpdateCheckResult.Idle -> "Version ${com.kipit.app.BuildConfig.VERSION_NAME}"
    }

    AppPage {
        item{SectionTitle("Preferences")}
        item{ActionRow("Budget meal cost",Centavos(s.config.mealAnchorCentavos).toPhpString(),Icons.Outlined.Restaurant,{open("meal")})}
        item{ActionRow("Savings goal",Centavos(s.config.savingsGoalCentavos).toPhpString(),Icons.Outlined.Flag,{open("goal")})}
        item{SectionTitle("On this device")}
        item{ActionRow("Payment capture",if(captureEnabled)"Enabled" else "Off",Icons.AutoMirrored.Outlined.ReceiptLong,{open("capture")})}
        item{ActionRow("Reminders",if(notifications)"Notifications allowed" else "Notifications off",Icons.Outlined.Notifications,{open("reminders")})}
        item{ActionRow("App updates",updateSubtitle,Icons.Outlined.SystemUpdate,{open("updates")})}
        item{ActionRow("About & data", "Offline storage",Icons.Outlined.Info,{open("about")})}
    }
    when(editor){
        "meal","goal"->MoneyEditor(if(editor=="meal")"Budget meal cost" else "Savings goal",if(editor=="meal")s.config.mealAnchorCentavos else s.config.savingsGoalCentavos,s.error,s.busy,dismiss={editor=""}){n->vm.saveSettings(if(editor=="meal")n else s.config.mealAnchorCentavos,if(editor=="goal")n else s.config.savingsGoalCentavos){editor=""}}
        "capture"->DetailSheet("Payment capture",{editor=""}){
            ActionRow(if(captureEnabled)"Enabled" else "Off","Optional notification access",Icons.Outlined.Notifications)
            Text("Read supported payment notifications locally. Every captured payment waits for your review.")
            Text("No notification content is sent online.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            ActionButton("Manage access",Icons.Outlined.Settings,{context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))},Modifier.fillMaxWidth(),primary=true)
        }
        "reminders"->DetailSheet("Reminders",{editor=""}){
            Text("Optional evening savings and weekly spending check-ins. Delivery time depends on Android power settings.")
            ActionButton(if(notifications)"Notification settings" else "Allow notifications",Icons.Outlined.Notifications,{
                if(Build.VERSION.SDK_INT>=33&&!notifications)permission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName))
            },Modifier.fillMaxWidth(),primary=true)
        }
        "updates"->DetailSheet("App updates",{editor=""}){
            ActionRow("Kipit","Installed: v${com.kipit.app.BuildConfig.VERSION_NAME}",Icons.Outlined.AccountBalanceWallet)
            when(val u = updateState){
                UpdateCheckResult.Idle->{
                    Text("Check GitHub releases for the latest updates, fixes, and features.")
                    ActionButton("Check for updates",Icons.Outlined.Refresh,{vm.checkForUpdates()},Modifier.fillMaxWidth(),primary=true)
                }
                UpdateCheckResult.Checking->{
                    Row(
                        modifier=Modifier.fillMaxWidth().padding(vertical=12.dp),
                        verticalAlignment=Alignment.CenterVertically,
                        horizontalArrangement=Arrangement.spacedBy(12.dp)
                    ){
                        CircularProgressIndicator(modifier=Modifier.size(24.dp),strokeWidth=2.dp)
                        Text("Checking GitHub for updates...")
                    }
                }
                is UpdateCheckResult.Available->{
                    ActionRow("New release available",u.newVersion,Icons.Outlined.NewReleases)
                    if(u.title.isNotBlank() && u.title != u.newVersion){
                        Text(u.title,style=MaterialTheme.typography.titleSmall)
                    }
                    if(u.releaseNotes.isNotBlank()){
                        Surface(
                            color=MaterialTheme.colorScheme.surfaceVariant,
                            shape=MaterialTheme.shapes.small,
                            modifier=Modifier.fillMaxWidth().heightIn(max=200.dp)
                        ){
                            LazyColumn(modifier=Modifier.padding(12.dp)){
                                item{Text(u.releaseNotes,style=MaterialTheme.typography.bodySmall)}
                            }
                        }
                    }
                    val targetUrl = u.downloadUrl ?: u.releaseUrl
                    ActionButton(
                        if(u.downloadUrl!=null)"Download APK" else "View on GitHub",
                        if(u.downloadUrl!=null)Icons.Outlined.Download else Icons.Outlined.OpenInBrowser,
                        {context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)))},
                        Modifier.fillMaxWidth(),
                        primary=true
                    )
                    ActionButton("Check again",Icons.Outlined.Refresh,{vm.checkForUpdates()},Modifier.fillMaxWidth(),primary=false)
                }
                is UpdateCheckResult.UpToDate->{
                    Text("You're using the latest release of Kipit (v${u.currentVersion}).")
                    ActionButton("Check again",Icons.Outlined.Refresh,{vm.checkForUpdates()},Modifier.fillMaxWidth(),primary=false)
                }
                is UpdateCheckResult.Error->{
                    Text("Unable to check: ${u.message}",color=MaterialTheme.colorScheme.error)
                    ActionButton("Retry check",Icons.Outlined.Refresh,{vm.checkForUpdates()},Modifier.fillMaxWidth(),primary=true)
                }
            }
        }
        "about"->DetailSheet("About & data",{editor=""}){
            ActionRow("Kipit","Version ${com.kipit.app.BuildConfig.VERSION_NAME}",Icons.Outlined.AccountBalanceWallet)
            Text("Your records stay on this device. Uninstalling removes them. Device backup is disabled; export and restore are not available yet.")
            if(com.kipit.app.BuildConfig.DEMO_DATA)Text("This is the separate sample app. These records are sample history.")
        }
    }
}
