package com.kipit.app.presentation.pulse

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.presentation.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun PulseDashboardScreen(s:UiState,add:()->Unit,explain:()->Unit,review:()->Unit,budgets:()->Unit,activity:()->Unit,opening:()->Unit) {
    val date=LocalDate.now()
    val today=remember(s.transactions,date){s.transactions.filter{it.type==TxType.EXPENSE&&it.date()==date}.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}
    val monthTotals=remember(s.transactions,date){s.transactions.filter{it.type==TxType.EXPENSE&&YearMonth.from(it.date())==YearMonth.from(date)}.groupBy{it.categoryId}.mapValues{(_,tx)->tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}}
    var detailId by remember{mutableStateOf<Long?>(null)}
    val safe=runCatching{s.safe}.getOrNull()
    AppPage {
        item {
            Column(Modifier.fillMaxWidth().padding(top=4.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Text(date.format(DateTimeFormatter.ofPattern("EEEE, d MMMM")),style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
                Text(if(safe!=null&&safe.value<0)"Over available budget" else "Available today",style=MaterialTheme.typography.titleMedium)
                Text(safe?.toPhpString()?:"Unavailable",style=MaterialTheme.typography.displaySmall,fontWeight=FontWeight.SemiBold,color=if(safe!=null&&safe.value<0)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                ActionButton("How it works",Icons.Outlined.Info,explain)
            }
        }
        item {
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
            MoneyRow("Spent today","",today)
            MoneyRow("Reserved savings","",s.config.lockedSavingsCentavos)
            HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)
        }
        item { ActionButton("Add expense",Icons.Outlined.Add,add,Modifier.fillMaxWidth(),primary=true) }
        if(s.transactions.isEmpty()&&s.wallets.all{it.balanceCentavos==0L})item {
            ActionRow("Set opening balances","Start with the money you already have.",Icons.Outlined.AccountBalanceWallet,opening)
        }
        val count=s.captures.size+s.loans.size+s.impulses.count{it.unlocksAtMs<=System.currentTimeMillis()}
        if(count>0)item{ActionRow("To review","$count ${if(count==1)"item" else "items"}",Icons.Outlined.Inbox,review)}
        item { SectionTitle("Budgets") }
        s.categories.take(3).forEach { c->item(key="budget${c.id}"){BudgetSummary(c,monthTotals[c.id]?:0)} }
        item { ActionButton("All budgets",Icons.Outlined.Tune,budgets,Modifier.fillMaxWidth()) }
        item { SectionTitle("Recent activity") }
        if(s.transactions.isEmpty())item{Text("Your first transaction will appear here.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)}
        s.transactions.take(3).forEach{tx->item(key="tx${tx.id}"){LedgerRow(tx,s){detailId=tx.id}}}
        if(s.transactions.isNotEmpty())item{ActionButton("All activity",Icons.AutoMirrored.Outlined.ReceiptLong,activity,Modifier.fillMaxWidth())}
    }
    s.transactions.find{it.id==detailId}?.let{TransactionDetail(it,s){detailId=null}}
}
@Composable fun BudgetSummary(c:CategoryEntity,spent:Long) {
    Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
        MoneyRow(c.name,if(c.monthlyLimitCentavos<=0)"No budget set" else if(spent>c.monthlyLimitCentavos)"${Centavos(Math.subtractExact(spent,c.monthlyLimitCentavos)).toPhpString()} over" else "${Centavos(Math.subtractExact(c.monthlyLimitCentavos,spent)).toPhpString()} left",spent)
        if(c.monthlyLimitCentavos>0) LinearProgressIndicator(progress={(spent.toFloat()/c.monthlyLimitCentavos).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp),color=if(spent>c.monthlyLimitCentavos)MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,trackColor=MaterialTheme.colorScheme.surfaceContainer)
    }
}
@Composable fun AllowanceSheet(s:UiState,dismiss:()->Unit) {
    val date=LocalDate.now()
    val liquid=runCatching{s.wallets.filter{it.type!=WalletType.CREDIT&&!it.isArchived}.sumMoney{it.balanceCentavos}}.getOrDefault(0L)
    val month=s.transactions.filter{it.type==TxType.EXPENSE&&YearMonth.from(it.date())==YearMonth.from(date)}
    val bills=runCatching{s.categories.filter{it.isFixedBill}.sumMoney{c->Math.subtractExact(c.monthlyLimitCentavos,month.filter{it.categoryId==c.id}.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}).coerceAtLeast(0)}}.getOrDefault(0L)
    DetailSheet("Available today",dismiss) {
        MoneyRow("Wallet cash","Cash, bank and e-wallets",liquid)
        MoneyRow("Bill reserve","Unspent fixed-bill budgets",bills)
        MoneyRow("Reserved savings","Already set aside",s.config.lockedSavingsCentavos)
        HorizontalDivider()
        Text("Cash minus bill reserves and savings, divided across ${date.lengthOfMonth()-date.dayOfMonth+1} remaining days this month.",style=MaterialTheme.typography.bodyMedium)
        Text("This is a rolling estimate based on your recorded balances. Money owed to you is not deducted again.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
