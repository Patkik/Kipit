package com.kipit.app.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kipit.app.presentation.pulse.*
import com.kipit.app.presentation.deeplab.*
import com.kipit.app.presentation.vault.*
import com.kipit.app.presentation.omnisheet.*
import kotlinx.coroutines.flow.receiveAsFlow

@OptIn(ExperimentalMaterial3Api::class)
@Composable fun KipitApp(vm:AppViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    var tab by rememberSaveable{mutableIntStateOf(0)}
    var settings by rememberSaveable{mutableStateOf(false)}
    var auxiliary by rememberSaveable{mutableStateOf("")}
    var sheet by rememberSaveable{mutableStateOf(false)}
    var entryMode by rememberSaveable{mutableStateOf("Expense")}
    var captureId by rememberSaveable{mutableStateOf<Long?>(null)}
    val snackbar=remember{SnackbarHostState()}
    val tabState=rememberSaveableStateHolder()
    LaunchedEffect(vm){vm.effects.receiveAsFlow().collect{snackbar.showSnackbar(it)}}
    fun entry(mode:String="Expense"){vm.clearError();captureId=null;entryMode=mode;sheet=true}
    BackHandler(settings || auxiliary.isNotEmpty() || tab!=0) { when { auxiliary.isNotEmpty()->auxiliary="";settings->settings=false;else->tab=0 } }
    val labels=listOf("Today","Activity","Wallets","Insights")
    val icons=listOf(Icons.Outlined.Today,Icons.AutoMirrored.Outlined.ReceiptLong,Icons.Outlined.AccountBalanceWallet,Icons.Outlined.BarChart)
    BoxWithConstraints {
        val wide=maxWidth>=600.dp
        Row(Modifier.fillMaxSize()) {
            if(wide&&!settings)NavigationRail { labels.forEachIndexed{i,label->NavigationRailItem(tab==i,{tab=i},icon={Icon(icons[i],null)},label={Text(label)})} }
            Scaffold(Modifier.weight(1f),topBar={
                TopAppBar(title={Column{Text(if(settings)"Settings" else labels[tab],fontWeight=FontWeight.SemiBold);if(com.kipit.app.BuildConfig.DEMO_DATA)Text("Sample history",style=MaterialTheme.typography.labelSmall,color=MaterialTheme.colorScheme.onSurfaceVariant)}},navigationIcon={if(settings)IconButton({settings=false}){Icon(Icons.AutoMirrored.Outlined.ArrowBack,"Back")}},actions={if(!settings)IconButton({settings=true}){Icon(Icons.Outlined.Settings,"Settings")}})
            },snackbarHost={SnackbarHost(snackbar)},bottomBar={
                if(!wide&&!settings)NavigationBar(containerColor=MaterialTheme.colorScheme.surface){labels.forEachIndexed{i,label->NavigationBarItem(tab==i,{tab=i},icon={Icon(icons[i],null)},label={Text(label)})}}
            },floatingActionButton={if(!settings && tab==1)FloatingActionButton(onClick={entry()},containerColor=MaterialTheme.colorScheme.primary,contentColor=MaterialTheme.colorScheme.onPrimary){Icon(Icons.Outlined.Add,"Add expense")}}) { padding->
                Box(Modifier.fillMaxSize().padding(padding)) {
                    if(!s.ready)Box(Modifier.fillMaxSize(),contentAlignment=Alignment.Center){CircularProgressIndicator()}
                    else tabState.SaveableStateProvider(if(settings)"settings" else "tab$tab") {
                    if(settings)SettingsScreen(s,vm) else when(tab) {
                        0->PulseDashboardScreen(s,{entry()},{entry("Income")},{auxiliary="calculation"},{vm.clearError();auxiliary="review"},{vm.clearError();auxiliary="budgets"},{tab=1},{entry("Opening balance")})
                        1->ActivityScreen(s)
                        2->VaultScreen(s,vm,{vm.clearError();auxiliary=it},{entry("Opening balance")},{entry("Income")},{entry("Expense")})
                        else->DeepLabAnalyticsScreen(s)
                    }
                    }
                }
            }
        }
    }
    if(s.ready)when(auxiliary){
        "calculation"->AllowanceSheet(s){auxiliary=""}
        "review"->ReviewSheet(s,vm,{vm.clearError();captureId=it.id;entryMode="Expense";sheet=true;auxiliary=""},{auxiliary="planned"}){auxiliary=""}
        "budgets"->BudgetSheet(s,vm){auxiliary=""}
        "savings"->SavingsSheet(s,vm){auxiliary=""}
        "planned"->PlannedSheet(s,vm){auxiliary=""}
    }
    if(sheet&&s.ready&&(captureId==null||s.captures.any{it.id==captureId}))OmniSheetModal(s,vm,s.captures.find{it.id==captureId},entryMode){sheet=false;captureId=null}
}
