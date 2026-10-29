package com.kipit.app.presentation

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import com.kipit.app.core.model.*
import com.kipit.app.data.local.entity.*
import java.time.*
import java.time.format.DateTimeFormatter

object Space { val xs=4.dp; val sm=8.dp; val md=12.dp; val lg=16.dp; val section=24.dp; val page=20.dp }
@Composable fun AppPage(content:LazyListScope.()->Unit) {
    Box(Modifier.fillMaxSize(),contentAlignment=Alignment.TopCenter) {
        LazyColumn(Modifier.widthIn(max=720.dp).fillMaxSize(),contentPadding=PaddingValues(Space.page,8.dp,Space.page,32.dp),verticalArrangement=Arrangement.spacedBy(12.dp),content=content)
    }
}
@Composable fun SectionTitle(title:String,subtitle:String?=null) {
    Column(Modifier.padding(top=12.dp,bottom=4.dp),verticalArrangement=Arrangement.spacedBy(4.dp)) {
        Text(title,style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold,modifier=Modifier.semantics{heading()})
        if(subtitle!=null) Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ActionButton(label:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier=Modifier,primary:Boolean=false,enabled:Boolean=true,busy:Boolean=false) {
    val body:@Composable RowScope.()->Unit={
        if(busy) CircularProgressIndicator(Modifier.size(20.dp),strokeWidth=2.dp,color=LocalContentColor.current)
        else Icon(icon,null,Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp));Text(label)
    }
    if(primary) Button(onClick,modifier.heightIn(min=52.dp),enabled=enabled&&!busy,shape=RoundedCornerShape(14.dp),content=body)
    else OutlinedButton(onClick,modifier.heightIn(min=48.dp),enabled=enabled&&!busy,shape=RoundedCornerShape(12.dp),content=body)
}
/** Routine money directions are peers; larger text stacks them without truncating labels. */
@Composable fun MoneyActions(onIncome:()->Unit,onExpense:()->Unit,enabled:Boolean=true) {
    if(LocalDensity.current.fontScale>1.3f)Column(Modifier.fillMaxWidth(),verticalArrangement=Arrangement.spacedBy(12.dp)) {
        MoneyDirectionButton("Money in","Income / Credit",Icons.Outlined.SouthWest,onIncome,Modifier.fillMaxWidth(),enabled)
        MoneyDirectionButton("Money out","Expense / Debit",Icons.Outlined.NorthEast,onExpense,Modifier.fillMaxWidth(),enabled)
    } else Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min),horizontalArrangement=Arrangement.spacedBy(12.dp)) {
        MoneyDirectionButton("Money in","Income / Credit",Icons.Outlined.SouthWest,onIncome,Modifier.weight(1f).fillMaxHeight(),enabled)
        MoneyDirectionButton("Money out","Expense / Debit",Icons.Outlined.NorthEast,onExpense,Modifier.weight(1f).fillMaxHeight(),enabled)
    }
}
@Composable private fun MoneyDirectionButton(title:String,subtitle:String,icon:ImageVector,onClick:()->Unit,modifier:Modifier,enabled:Boolean) {
    Surface(onClick=onClick,enabled=enabled,modifier=modifier.heightIn(min=88.dp),shape=RoundedCornerShape(14.dp),color=MaterialTheme.colorScheme.primaryContainer,contentColor=MaterialTheme.colorScheme.onPrimaryContainer,border=BorderStroke(1.dp,MaterialTheme.colorScheme.outline)) {
        Row(Modifier.padding(16.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(8.dp)) {
            Icon(icon,null,Modifier.size(22.dp))
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(4.dp)) {
                Text(title,style=MaterialTheme.typography.titleMedium)
                Text(subtitle,style=MaterialTheme.typography.bodySmall)
            }
        }
    }
}
@Composable fun ActionRow(title:String,subtitle:String?=null,icon:ImageVector,onClick:(()->Unit)?=null,value:String?=null,selected:Boolean?=null,enabled:Boolean=true) {
    val selection=selected
    val large=LocalDensity.current.fontScale>1.3f
    Surface(Modifier.fillMaxWidth().semantics{selection?.let{this.selected=it}}.then(if(onClick!=null)Modifier.clickable(enabled=enabled,role=if(selected!=null)Role.RadioButton else Role.Button,onClick=onClick) else Modifier),shape=RoundedCornerShape(14.dp),color=if(selected==true)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,border=BorderStroke(1.dp,if(selected==true)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) {
        Row(Modifier.padding(16.dp).heightIn(min=24.dp),verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Icon(icon,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.primary)
            Column(Modifier.weight(1f),verticalArrangement=Arrangement.spacedBy(3.dp)) {
                Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium)
                if(subtitle!=null)Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                if(value!=null&&large)Text(value,style=MaterialTheme.typography.titleMedium)
            }
            if(value!=null&&!large)Text(value,Modifier.widthIn(max=160.dp),style=MaterialTheme.typography.titleMedium,fontWeight=FontWeight.SemiBold)
            if(onClick!=null)Icon(if(selected==true)Icons.Outlined.Check else Icons.Outlined.ChevronRight,null,Modifier.size(20.dp))
        }
    }
}
@Composable fun MoneyRow(title:String,subtitle:String,amount:Long,action:(@Composable ()->Unit)?=null) {
    val large=LocalDensity.current.fontScale>1.3f
    Column(Modifier.fillMaxWidth().padding(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.spacedBy(12.dp)) {
            Column(Modifier.weight(1f)) { Text(title,style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium);if(subtitle.isNotEmpty())Text(subtitle,style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            if(!large)Text(Centavos(amount).toPhpString(),Modifier.widthIn(max=180.dp),style=MaterialTheme.typography.titleMedium)
        }
        if(large)Text(Centavos(amount).toPhpString(),style=MaterialTheme.typography.titleMedium)
        action?.invoke()
    }
}
@Composable fun EmptyState(title:String,detail:String,icon:ImageVector) {
    Column(Modifier.fillMaxWidth().padding(vertical=20.dp),verticalArrangement=Arrangement.spacedBy(8.dp)) {
        Icon(icon,null,tint=MaterialTheme.colorScheme.onSurfaceVariant)
        Text(title,style=MaterialTheme.typography.titleMedium)
        Text(detail,style=MaterialTheme.typography.bodyMedium,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
@Composable fun ErrorNotice(message:String?) { if(message!=null)Surface(modifier=Modifier.semantics{liveRegion=LiveRegionMode.Polite;error(message)},color=MaterialTheme.colorScheme.errorContainer,shape=RoundedCornerShape(12.dp)) { Row(Modifier.padding(12.dp),horizontalArrangement=Arrangement.spacedBy(8.dp)){Icon(Icons.Outlined.ErrorOutline,null);Text(message,style=MaterialTheme.typography.bodyMedium)} } }
@OptIn(ExperimentalMaterial3Api::class)
@Composable fun DetailSheet(title:String,onDismiss:()->Unit,footer:(@Composable ()->Unit)?=null,content:@Composable ColumnScope.()->Unit) {
    ModalBottomSheet(onDismissRequest=onDismiss,sheetState=rememberModalBottomSheetState(skipPartiallyExpanded=true),containerColor=MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().fillMaxHeight(.9f).imePadding()) {
            Row(Modifier.fillMaxWidth().padding(horizontal=20.dp),verticalAlignment=Alignment.CenterVertically) {
                Text(title,Modifier.weight(1f).semantics{heading()},style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.SemiBold)
                IconButton(onDismiss){Icon(Icons.Outlined.Close,"Close $title")}
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(20.dp),verticalArrangement=Arrangement.spacedBy(16.dp),content=content)
            if(footer!=null)Surface(color=MaterialTheme.colorScheme.surface){Box(Modifier.fillMaxWidth().padding(horizontal=20.dp,vertical=12.dp)){footer()}}
        }
    }
}
fun TransactionEntity.date():LocalDate=Instant.ofEpochMilli(timestampEpochMs).atZone(ZoneId.systemDefault()).toLocalDate()
fun TxType.label()=when(this){TxType.EXPENSE->"Expense";TxType.INCOME->"Income";TxType.TRANSFER->"Transfer";TxType.OPENING->"Opening balance"}
fun categoryIcon(name:String):ImageVector=when { name.contains("Food",true)||name.contains("Grocer",true)->Icons.Outlined.Restaurant;name.contains("Transport",true)->Icons.Outlined.DirectionsBus;name.contains("Util",true)->Icons.Outlined.Bolt;name.contains("Tech",true)->Icons.Outlined.Devices;else->Icons.Outlined.Category }
fun walletIcon(type:WalletType)=when(type){WalletType.CASH->Icons.Outlined.Payments;WalletType.E_WALLET->Icons.Outlined.Smartphone;else->Icons.Outlined.AccountBalance}
@Composable fun LedgerRow(tx:TransactionEntity,s:UiState,onClick:()->Unit) {
    val category=s.categories.find{it.id==tx.categoryId};val wallet=s.wallets.find{it.id==tx.sourceWalletId}
    val large=LocalDensity.current.fontScale>1.3f
    val icon=when(tx.type){TxType.EXPENSE->categoryIcon(category?.name?:"");TxType.TRANSFER->Icons.Outlined.SwapHoriz;TxType.INCOME->Icons.Outlined.SouthWest;TxType.OPENING->Icons.Outlined.AccountBalanceWallet}
    val prefix=when(tx.type){TxType.EXPENSE->"−";TxType.INCOME,TxType.OPENING->"+";else->""}
    val amount=prefix+Centavos(tx.amountCentavos).toPhpString()
    Column(Modifier.fillMaxWidth().clickable(role=Role.Button,onClick=onClick).padding(vertical=12.dp)) {
        Row(horizontalArrangement=Arrangement.spacedBy(12.dp),verticalAlignment=Alignment.CenterVertically){
            Icon(icon,null,Modifier.size(22.dp),tint=MaterialTheme.colorScheme.onSurfaceVariant)
            Column(Modifier.weight(1f)) { Text(tx.note.ifBlank{category?.name?:tx.type.label()},style=MaterialTheme.typography.bodyLarge,fontWeight=FontWeight.Medium);Text("${tx.type.label()} · ${wallet?.name?:"Wallet"}",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant) }
            if(!large)Text(amount,Modifier.widthIn(max=160.dp),style=MaterialTheme.typography.titleMedium,color=if(tx.type==TxType.INCOME)MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
        }
        if(large)Text(amount,Modifier.align(Alignment.End).padding(top=4.dp),style=MaterialTheme.typography.titleMedium)
    }
}
@Composable fun TransactionDetail(tx:TransactionEntity,s:UiState,onDismiss:()->Unit) {
    DetailSheet("Transaction details",onDismiss) {
        Text(Centavos(tx.amountCentavos).toPhpString(),style=MaterialTheme.typography.headlineLarge,fontWeight=FontWeight.SemiBold)
        ActionRow(tx.type.label(),tx.date().format(DateTimeFormatter.ofPattern("d MMMM yyyy")),Icons.AutoMirrored.Outlined.ReceiptLong)
        ActionRow(if(tx.type in listOf(TxType.INCOME,TxType.OPENING))"To wallet" else "From wallet",s.wallets.find{it.id==tx.sourceWalletId}?.name?:"Wallet ${tx.sourceWalletId}",Icons.Outlined.AccountBalanceWallet)
        tx.destinationWalletId?.let{id->ActionRow("To",s.wallets.find{it.id==id}?.name?:"Wallet $id",Icons.Outlined.SwapHoriz)}
        if(tx.categoryId!=null)ActionRow("Category",s.categories.find{it.id==tx.categoryId}?.name?:"Category",Icons.Outlined.Category)
        if(tx.feeCentavos>0)MoneyRow("Fee","Separate from principal",tx.feeCentavos)
        if(tx.note.isNotBlank())Text(tx.note)
        if(tx.tagsCsv.isNotBlank())Text(tx.tagsCsv.split(',').joinToString(" "){"#$it"},color=MaterialTheme.colorScheme.primary)
        if(tx.type==TxType.EXPENSE)Text("Split entries show your personal share. Cash advanced to others is tracked separately.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
