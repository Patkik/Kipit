package com.kipit.app.presentation.vault

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.presentation.*
import java.time.*

@Composable fun VaultScreen(s:UiState,vm:AppViewModel,open:(String)->Unit,opening:()->Unit,income:()->Unit,expense:()->Unit) {
    var wallet by rememberSaveable{mutableStateOf<Long?>(null)}
    AppPage {
        item{MoneyActions(income,expense,enabled=!s.busy)}
        item{SectionTitle("Balances")}
        items(s.wallets,key={it.id}){w->ActionRow(w.name,w.type.name.lowercase().replace('_',' '),walletIcon(w.type),{wallet=w.id},value=Centavos(w.balanceCentavos).toPhpString())}
        if(s.transactions.isEmpty())item{ActionButton("Set opening balance",Icons.Outlined.Add,opening,Modifier.fillMaxWidth())}
        item{SectionTitle("Plan your money")}
        item{ActionRow("Savings","${Centavos(s.config.lockedSavingsCentavos).toPhpString()} reserved",Icons.Outlined.Savings,{open("savings")})}
        item{ActionRow("Planned purchases","${s.impulses.size} waiting",Icons.Outlined.Schedule,{open("planned")})}
        item{ActionRow("Manage budgets","Monthly category limits",Icons.Outlined.Tune,{open("budgets")})}
    }
    s.wallets.find{it.id==wallet}?.let{w->DetailSheet(w.name,{wallet=null}){
        Text(Centavos(w.balanceCentavos).toPhpString(),style=MaterialTheme.typography.headlineLarge)
        SectionTitle("Recent activity")
        val txs=s.transactions.filter{it.sourceWalletId==w.id||it.destinationWalletId==w.id}.take(10)
        if(txs.isEmpty())Text("No transactions in this wallet.")
        txs.forEach{tx->MoneyRow(tx.note.ifBlank{tx.type.label()},"${tx.date()} · ${tx.type.label()}",tx.amountCentavos)}
    }}
}
@Composable fun BudgetSheet(s:UiState,vm:AppViewModel,dismiss:()->Unit) {
    var edit by rememberSaveable{mutableStateOf<Long?>(null)}
    val month=YearMonth.now()
    val totals=remember(s.transactions,month){s.transactions.filter{it.type==TxType.EXPENSE&&YearMonth.from(it.date())==month}.groupBy{it.categoryId}.mapValues{(_,tx)->tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}}
    val category=s.categories.find{it.id==edit}
    if(category!=null)MoneyEditor("${category.name} budget",category.monthlyLimitCentavos,s.error,s.busy,true,{edit=null}){n->vm.setBudget(category.id,n){edit=null}}
    else DetailSheet("Monthly budgets",dismiss){
        Text("Choose a category to edit its limit.",style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
        s.categories.forEach{c->ActionRow(c.name,if(c.monthlyLimitCentavos==0L)"No limit · ${Centavos(totals[c.id]?:0L).toPhpString()} spent" else "${Centavos(totals[c.id]?:0L).toPhpString()} of ${Centavos(c.monthlyLimitCentavos).toPhpString()}",categoryIcon(c.name),{vm.clearError();edit=c.id})}
    }
}
@Composable fun MoneyEditor(title:String,initial:Long,error:String?,busy:Boolean,allowZero:Boolean=false,dismiss:()->Unit,save:(Long)->Unit) {
    var value by rememberSaveable(title){mutableStateOf(java.math.BigDecimal.valueOf(initial,2).toPlainString())}
    val parsed=runCatching{Centavos.parse(value).value}.getOrNull()
    val valid=parsed!=null && if(allowZero)parsed>=0 else parsed>0
    DetailSheet(title,{if(!busy)dismiss()},footer={ActionButton("Save changes",Icons.Outlined.Check,{save(parsed!!)},Modifier.fillMaxWidth(),primary=true,enabled=valid,busy=busy)}) {
        OutlinedTextField(value,{value=it},label={Text("Amount (PHP)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Decimal),singleLine=true,isError=!valid,modifier=Modifier.fillMaxWidth())
        if(!valid)Text(if(allowZero)"Enter zero or a positive amount." else "Enter a positive amount.",color=MaterialTheme.colorScheme.error)
        if(allowZero)Text("Zero removes the monthly limit.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        ErrorNotice(error)
    }
}
@Composable fun SavingsSheet(s:UiState,vm:AppViewModel,dismiss:()->Unit) {
    var confirm by remember{mutableStateOf(false)}
    DetailSheet("Savings",dismiss){
        MoneyRow("Reserved","From your existing cash",s.config.lockedSavingsCentavos)
        MoneyRow("Goal","",s.config.savingsGoalCentavos)
        LinearProgressIndicator(progress={(s.config.lockedSavingsCentavos.toFloat()/s.config.savingsGoalCentavos.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth())
        Text("Reserve half of today's calculated surplus. Your wallet balances stay the same.",style=MaterialTheme.typography.bodyMedium)
        ActionButton("Reserve today's surplus",Icons.Outlined.Savings,{vm.clearError();confirm=true},Modifier.fillMaxWidth(),primary=true,enabled=s.config.lastSweepDate!=LocalDate.now().toString(),busy=s.busy)
        if(s.config.lastSweepDate==LocalDate.now().toString())Text("Today's savings reserve is complete.",style=MaterialTheme.typography.bodySmall)
        ErrorNotice(s.error)
    }
    if(confirm)AlertDialog(onDismissRequest={confirm=false},title={Text("Reserve surplus?")},text={Text("This sets aside half of the available daily surplus, capped by unreserved cash after bills. It reduces your spending allowance.")},confirmButton={ActionButton("Reserve",Icons.Outlined.Check,{vm.dispatch(UiIntent.Sweep);confirm=false},primary=true)},dismissButton={ActionButton("Cancel",Icons.Outlined.Close,{confirm=false})})
}
@Composable fun PlannedSheet(s:UiState,vm:AppViewModel,dismiss:()->Unit) {
    var confirmId by rememberSaveable{mutableStateOf<Long?>(null)}
    var buying by rememberSaveable{mutableStateOf(false)}
    var walletId by rememberSaveable{mutableStateOf(s.wallets.firstOrNull()?.id?:0L)}
    val now by produceState(System.currentTimeMillis()){while(true){value=System.currentTimeMillis();kotlinx.coroutines.delay(60000)}}
    val item=s.impulses.find{it.id==confirmId}
    DetailSheet("Planned purchases",dismiss){
        if(s.impulses.isEmpty())EmptyState("Nothing waiting","Choose Planned purchase under Other transaction to give a purchase seven days.",Icons.Outlined.Schedule)
        s.impulses.forEach{impulse->
            val remaining=(impulse.unlocksAtMs-now).coerceAtLeast(0)
            MoneyRow(impulse.itemName,if(remaining>0)"${remaining/86400000}d ${(remaining/3600000)%24}h remaining" else "Ready to review",impulse.amountCentavos)
            if(remaining==0L){
                ActionButton("Skip & reserve",Icons.Outlined.Savings,{vm.clearError();buying=false;confirmId=impulse.id},Modifier.fillMaxWidth(),enabled=!s.busy)
                ActionButton("Review purchase",Icons.Outlined.ShoppingBag,{vm.clearError();buying=true;confirmId=impulse.id},Modifier.fillMaxWidth(),enabled=!s.busy)
            }
            HorizontalDivider()
        }
        ErrorNotice(s.error)
    }
    if(item!=null)DetailSheet(if(buying)"Buy ${item.itemName}" else "Skip ${item.itemName}",{if(!s.busy)confirmId=null},footer={ActionButton(if(buying)"Buy purchase" else "Skip & reserve",if(buying)Icons.Outlined.ShoppingBag else Icons.Outlined.Savings,{vm.dispatch(UiIntent.Impulse(item.id,buying,walletId));confirmId=null},Modifier.fillMaxWidth(),primary=true,busy=s.busy)}) {
        Text(Centavos(item.amountCentavos).toPhpString(),style=MaterialTheme.typography.headlineMedium)
        if(buying){Text("Pay from");s.wallets.forEach{w->ActionRow(w.name,null,walletIcon(w.type),{walletId=w.id},selected=walletId==w.id)}}
        else Text("Reserve this amount from your existing cash. This does not add money to your wallets.")
    }
}
@Composable fun ReviewSheet(s:UiState,vm:AppViewModel,openCapture:(PendingCaptureEntity)->Unit,openPlanned:()->Unit,dismiss:()->Unit) {
    var loanId by rememberSaveable{mutableStateOf<Long?>(null)}
    DetailSheet("To review",dismiss){
        if(s.captures.isEmpty()&&s.loans.isEmpty()&&s.impulses.none{it.unlocksAtMs<=System.currentTimeMillis()})EmptyState("All caught up","Payments and money owed to you will appear here.",Icons.Outlined.CheckCircle)
        s.captures.forEach{capture->
            MoneyRow("Captured payment","Awaiting your confirmation",capture.amountCentavos)
            ActionButton("Review payment",Icons.AutoMirrored.Outlined.ReceiptLong,{vm.clearError();openCapture(capture)},Modifier.fillMaxWidth(),primary=true,enabled=!s.busy)
            ActionButton("Dismiss capture",Icons.Outlined.Close,{vm.dismissCapture(capture.id)},Modifier.fillMaxWidth(),enabled=!s.busy)
            HorizontalDivider()
        }
        s.loans.forEach{loan->MoneyRow(loan.debtorName,"Owes you",loan.amountOwedCentavos);ActionButton("Record repayment",Icons.Outlined.Check,{loanId=loan.id},Modifier.fillMaxWidth(),enabled=!s.busy);HorizontalDivider()}
        if(s.impulses.any{it.unlocksAtMs<=System.currentTimeMillis()})ActionButton("Review planned purchases",Icons.Outlined.Schedule,openPlanned,Modifier.fillMaxWidth())
        ErrorNotice(s.error)
    }
    s.loans.find{it.id==loanId}?.let{loan->AlertDialog(onDismissRequest={loanId=null},title={Text("Record repayment?")},text={Text("${loan.debtorName} paid ${Centavos(loan.amountOwedCentavos).toPhpString()}. The cash returns to the original payment wallet.")},confirmButton={ActionButton("Record",Icons.Outlined.Check,{vm.dispatch(UiIntent.Settle(loan.id));loanId=null},primary=true)},dismissButton={ActionButton("Cancel",Icons.Outlined.Close,{loanId=null})})}
}
