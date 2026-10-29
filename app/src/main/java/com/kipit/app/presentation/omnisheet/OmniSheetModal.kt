package com.kipit.app.presentation.omnisheet

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.kipit.app.presentation.*
import com.kipit.app.core.model.*
import com.kipit.app.core.util.*
import com.kipit.app.data.local.entity.*
import com.kipit.app.domain.usecase.*
import java.time.*

@Composable fun OmniSheetModal(s:UiState,vm:AppViewModel,capture:PendingCaptureEntity?,initialMode:String="Expense",dismiss:()->Unit) {
    var expression by rememberSaveable{mutableStateOf(capture?.let{java.math.BigDecimal.valueOf(it.amountCentavos,2).toPlainString()}?:"")}
    var note by rememberSaveable{mutableStateOf(capture?.merchant?:"")}
    var mode by rememberSaveable{mutableStateOf(if(capture==null)initialMode else "Expense")}
    val provider=when(capture?.packageName){"com.globe.gcash.android"->"GCash";"com.paymaya"->"Maya";"com.bpi.ng.app"->"BPI";else->""}
    var wallet by rememberSaveable{mutableLongStateOf(if(capture==null)s.wallets.firstOrNull()?.id?:0L else s.wallets.find{provider.isNotEmpty()&&it.name.contains(provider,true)}?.id?:0L)}
    var destination by rememberSaveable{mutableLongStateOf(s.wallets.lastOrNull()?.id?:0L)}
    var category by rememberSaveable{mutableLongStateOf(s.predicted.firstOrNull()?:s.categories.firstOrNull()?.id?:0L)}
    var fee by rememberSaveable{mutableStateOf(false)}
    var debtor by rememberSaveable{mutableStateOf("")}
    var split by rememberSaveable{mutableStateOf("2")}
    var batch by rememberSaveable{mutableStateOf("")}
    var noteOpen by rememberSaveable{mutableStateOf(capture!=null)}
    var calculator by rememberSaveable{mutableStateOf(false)}
    var picker by rememberSaveable{mutableStateOf("")}
    val batchPreview=remember(batch,s.wallets,s.categories){MicroSyntaxLexer.parse(batch,s.wallets,s.categories)}
    val operationKey=rememberSaveable{java.util.UUID.randomUUID().toString()}
    val result=InlineMathEvaluator.evaluate(expression)
    val amount=result.getOrDefault(Centavos(0))
    val n=split.toLongOrNull()?.takeIf{it in 2..100}
    val personal=if(mode=="Split")amount.value/(n?:2L) else amount.value
    val selectedWallet=s.wallets.find{it.id==wallet}
    val selectedCategory=s.categories.find{it.id==category}
    val transferFee=if(mode=="Transfer"&&fee)1500L else capture?.feeCentavos?:0L
    val type=when(mode){"Income"->TxType.INCOME;"Opening balance"->TxType.OPENING;"Transfer"->TxType.TRANSFER;else->TxType.EXPENSE}
    val categorised=mode in listOf("Expense","Split","Park")
    val preview=runCatching {
        val now=ZonedDateTime.now()
        if(mode=="Park")s.safe else previewTransaction(s.wallets,s.categories,s.transactions,s.config,TransactionEntity(type=type,amountCentavos=personal,feeCentavos=transferFee,sourceWalletId=wallet,destinationWalletId=if(mode=="Transfer")destination else null,categoryId=if(categorised)category else null,timestampEpochMs=now.toInstant().toEpochMilli(),hourOfDay=now.hour,dayOfWeek=now.dayOfWeek.value,isWeekend=now.dayOfWeek.value>=6),if(mode=="Split")Math.subtractExact(amount.value,personal) else 0L)
    }
    val valid=amount.value>0&&result.isSuccess&&preview.isSuccess&&selectedWallet!=null&&(!categorised||selectedCategory!=null)&&(mode!="Split"||(n!=null&&personal>0&&debtor.isNotBlank()))&&(mode!="Transfer"||(destination!=wallet&&s.wallets.any{it.id==destination}))&&(mode!="Park"||note.isNotBlank())
    fun commit() {
        if(!valid)return
        if(mode=="Park"){vm.park(note,amount.value,category,dismiss);return}
        vm.log(personal,type,wallet,if(mode=="Transfer")destination else null,if(categorised)category else null,note,transferFee,if(mode=="Split")Math.subtractExact(amount.value,personal) else 0L,debtor,capture?.id,operationKey,dismiss)
    }
    val title=if(capture!=null)"Review payment" else when(mode){"Expense"->"Add expense";"Park"->"Planned purchase";"Opening balance"->"Opening balance";else->"Add ${mode.lowercase()}"}
    val saveLabel=if(capture!=null)"Confirm payment" else when(mode){"Expense"->"Save expense";"Park"->"Park for 7 days";"Transfer"->"Save transfer";"Split"->"Save split";"Income"->"Save income";else->"Save opening balance"}
    DetailSheet(title,{if(!s.busy)dismiss()},footer={ActionButton(saveLabel,Icons.Outlined.Check,{commit()},Modifier.fillMaxWidth(),primary=true,enabled=valid,busy=s.busy)}) {
        OutlinedTextField(expression,{expression=it},label={Text("Amount (PHP)")},keyboardOptions=KeyboardOptions(keyboardType=if(calculator)KeyboardType.Text else KeyboardType.Decimal),isError=result.isFailure,textStyle=MaterialTheme.typography.headlineMedium.copy(fontWeight=FontWeight.Medium),singleLine=true,modifier=Modifier.fillMaxWidth())
        ErrorNotice(s.error)
        if(result.isFailure)ErrorNotice("Check the amount or calculation.")
        if(categorised)ActionRow("Category",selectedCategory?.name?:"Choose category",categoryIcon(selectedCategory?.name?:""),{picker="category"})
        if(mode!="Park")ActionRow(if(mode=="Income"||mode=="Opening balance")"To wallet" else "From wallet",selectedWallet?.let{"${it.name} · ${Centavos(it.balanceCentavos).toPhpString()}"}?:"Choose wallet",Icons.Outlined.AccountBalanceWallet,{picker="wallet"})
        if(mode=="Transfer") {
            ActionRow("To wallet",s.wallets.find{it.id==destination&&it.id!=wallet}?.name?:"Choose destination",Icons.Outlined.SwapHoriz,{picker="destination"})
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween){Text("Add ₱15 fee");Switch(fee,{fee=it},modifier=Modifier.semantics{contentDescription="Add 15 peso transfer fee"})}
        }
        if(transferFee>0)MoneyRow("Fee","Added to the payment",transferFee)
        if(mode=="Split") {
            OutlinedTextField(debtor,{debtor=it},label={Text("Who owes you?")},singleLine=true,modifier=Modifier.fillMaxWidth())
            OutlinedTextField(split,{split=it},label={Text("People, including you (2–100)")},keyboardOptions=KeyboardOptions(keyboardType=KeyboardType.Number),singleLine=true,isError=n==null,modifier=Modifier.fillMaxWidth())
            MoneyRow("Your share","Full payment leaves your wallet",personal)
        }
        if(mode=="Park"||noteOpen)OutlinedTextField(note,{note=it},label={Text(if(mode=="Park")"Purchase name" else "Note")},modifier=Modifier.fillMaxWidth())
        if(mode=="Opening balance")Text("For an unused wallet. Existing money is excluded from earned income.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(mode=="Park")Text("No money moves. Review the purchase after seven days.",style=MaterialTheme.typography.bodyMedium)
        else if(amount.value>0&&preview.isSuccess) {
            HorizontalDivider()
            MoneyRow("Available after this","Rolling daily estimate",preview.getOrDefault(Centavos(0)).value)
        }
        if(preview.isFailure)ErrorNotice("This amount is too large to calculate safely.")
        if(capture!=null)Text("Captured locally. Check the wallet, amount and fee before confirming.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
        if(calculator){
            listOf("7","8","9","+","4","5","6","−","1","2","3","×",".","0","⌫","÷").chunked(4).forEach{row->Row(horizontalArrangement=Arrangement.spacedBy(8.dp)){row.forEach{key->OutlinedButton(onClick={expression=if(key=="⌫")expression.dropLast(1) else expression+when(key){"−"->"-";"×"->"*";"÷"->"/";else->key}},modifier=Modifier.weight(1f).heightIn(min=48.dp),contentPadding=PaddingValues(0.dp)){Text(key)}}}}
        }
        if(capture==null)ActionRow("Other transaction",null,Icons.Outlined.SwapHoriz,{picker="mode"})
        ActionRow("More options",null,Icons.Outlined.MoreHoriz,{picker="tools"})
    }
    if(picker.isNotEmpty())DetailSheet(when(picker){"category"->"Choose category";"wallet","destination"->"Choose wallet";"mode"->"Other transaction";"batch"->"Multiple expenses";"repeats"->"Repeat expense";else->"More options"},{if(!s.busy)picker=""},footer=if(picker=="batch")({ActionButton("Record batch",Icons.Outlined.Check,{vm.batch(batch,dismiss)},Modifier.fillMaxWidth(),primary=true,enabled=batchPreview.isSuccess&&batchPreview.getOrNull()?.all{it.personal.value>0}==true,busy=s.busy)}) else null) {
        when(picker) {
            "category"->s.categories.sortedBy{c->s.predicted.indexOf(c.id).let{if(it<0)100 else it}}.forEach{c->ActionRow(c.name,null,categoryIcon(c.name),{category=c.id;picker=""},selected=category==c.id)}
            "wallet","destination"->s.wallets.filter{it.type!=WalletType.CREDIT&&(picker!="destination"||it.id!=wallet)}.forEach{w->ActionRow(w.name,Centavos(w.balanceCentavos).toPhpString(),walletIcon(w.type),{if(picker=="destination")destination=w.id else {wallet=w.id;if(destination==wallet)destination=s.wallets.firstOrNull{it.id!=wallet}?.id?:0L};picker=""},selected=if(picker=="destination")destination==w.id else wallet==w.id)}
            "mode"->listOf("Expense","Income","Transfer","Split","Park","Opening balance").forEach{m->ActionRow(if(m=="Park")"Planned purchase" else m,null,when(m){"Income"->Icons.Outlined.SouthWest;"Transfer"->Icons.Outlined.SwapHoriz;"Split"->Icons.Outlined.Group;"Park"->Icons.Outlined.Schedule;"Opening balance"->Icons.Outlined.AccountBalanceWallet;else->Icons.AutoMirrored.Outlined.ReceiptLong},{mode=m;vm.clearError();picker=""},selected=mode==m)}
            "tools"->{
                ActionRow(if(noteOpen)"Hide note" else "Add note",null,Icons.Outlined.EditNote,{noteOpen=!noteOpen;picker=""})
                ActionRow(if(calculator)"Hide calculator" else "Calculator",null,Icons.Outlined.Calculate,{calculator=!calculator;picker=""})
                if(capture==null){ActionRow("Repeat expense",null,Icons.Outlined.Replay,{picker="repeats"});ActionRow("Multiple expenses",null,Icons.AutoMirrored.Outlined.PlaylistAdd,{picker="batch"})}
            }
            "repeats"->{if(s.macros.isEmpty())EmptyState("No repeats yet","Frequent expenses appear after you record them a few times.",Icons.Outlined.Replay);s.macros.forEach{m->ActionRow(m.note.ifBlank{"Repeat expense"},Centavos(m.amountCentavos).toPhpString(),Icons.Outlined.Replay,{expression=java.math.BigDecimal.valueOf(m.amountCentavos,2).toPlainString();wallet=m.sourceWalletId;category=m.categoryId?:category;note=m.note;mode="Expense";picker=""})}}
            "batch"->{
                Text("Separate expenses with commas. Review your text before recording; the whole batch saves together.")
                OutlinedTextField(batch,{batch=it},label={Text("Expenses")},placeholder={Text("15 Transport, 120 Food gcash")},modifier=Modifier.fillMaxWidth(),minLines=3)
                if(batch.isNotBlank()&&batchPreview.isFailure)ErrorNotice("Check each amount and split. Separate entries with commas.")
                if(batchPreview.isSuccess){
                    SectionTitle("Review entries")
                    batchPreview.getOrThrow().forEach{e->MoneyRow(e.note.ifBlank{"Expense"},"${s.wallets.find{it.id==e.walletId}?.name?:"Wallet"} · ${s.categories.find{it.id==e.categoryId}?.name?:s.categories.firstOrNull()?.name?:"Category"}",e.amount.value);if(e.owed.value>0)MoneyRow("Your share","${Centavos(e.owed.value).toPhpString()} owed by others",e.personal.value)}
                }
                Text("Unmatched categories use the first category shown in the review.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
                ErrorNotice(s.error)
            }
        }
    }
}
