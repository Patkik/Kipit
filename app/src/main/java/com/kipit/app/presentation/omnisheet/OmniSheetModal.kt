package com.kipit.app.presentation.omnisheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
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
    val initialCatId = remember(s.predicted, s.categories, initialMode, capture) {
        if (capture == null && initialMode == "Income") 0L
        else s.predicted.firstOrNull() ?: s.categories.firstOrNull()?.id ?: 0L
    }
    var category by rememberSaveable{mutableLongStateOf(initialCatId)}
    var categoryText by rememberSaveable{
        mutableStateOf(
            if (capture == null && initialMode == "Income") ""
            else s.categories.find { it.id == initialCatId }?.name ?: ""
        )
    }
    var categorySearch by rememberSaveable{mutableStateOf("")}
    var fee by rememberSaveable{mutableStateOf(false)}
    var debtor by rememberSaveable{mutableStateOf("")}
    var split by rememberSaveable{mutableStateOf("2")}
    var batch by rememberSaveable{mutableStateOf("")}
    var noteOpen by rememberSaveable{mutableStateOf(capture!=null)}
    var calculator by rememberSaveable{mutableStateOf(false)}
    var picker by rememberSaveable{mutableStateOf("")}
    var showAiPrompt by rememberSaveable{mutableStateOf(false)}
    var aiPromptInput by rememberSaveable{mutableStateOf("")}
    var aiError by rememberSaveable{mutableStateOf<String?>(null)}
    val isAiBusy by vm.isAiBusy.collectAsState()

    LaunchedEffect(mode) {
        if (mode == "Income") {
            if (categoryText in listOf("Food", "Transport", "Utilities", "Tech & Gaming", "Groceries", "Subscriptions")) {
                categoryText = ""
                category = 0L
            }
        } else if (mode in listOf("Expense", "Split", "Park")) {
            if (categoryText.isBlank() && category == 0L) {
                val defId = s.predicted.firstOrNull() ?: s.categories.firstOrNull()?.id ?: 0L
                category = defId
                categoryText = s.categories.find { it.id == defId }?.name ?: ""
            }
        }
    }

    val batchPreview=remember(batch,s.wallets,s.categories){MicroSyntaxLexer.parse(batch,s.wallets,s.categories)}
    val operationKey=rememberSaveable{java.util.UUID.randomUUID().toString()}
    val result=InlineMathEvaluator.evaluate(expression)
    val amount=result.getOrDefault(Centavos(0))
    val n=split.toLongOrNull()?.takeIf{it in 2..100}
    val personal=if(mode=="Split")amount.value/(n?:2L) else amount.value
    val selectedWallet=s.wallets.find{it.id==wallet}
    val selectedCategory=s.categories.find{it.id==category} ?: s.categories.find{it.name.trim().equals(categoryText.trim(), ignoreCase = true)}
    val transferFee=if(mode=="Transfer"&&fee)1500L else capture?.feeCentavos?:0L
    val type=when(mode){"Income"->TxType.INCOME;"Opening balance"->TxType.OPENING;"Transfer"->TxType.TRANSFER;else->TxType.EXPENSE}
    val categorised=mode in listOf("Expense","Income","Split","Park")
    val preview=runCatching {
        val now=ZonedDateTime.now()
        val previewCatId = if (categorised) {
            if (category != 0L) category
            else selectedCategory?.id ?: s.categories.firstOrNull()?.id
        } else null
        if(mode=="Park")s.safe else previewTransaction(s.wallets,s.categories,s.transactions,s.config,TransactionEntity(type=type,amountCentavos=personal,feeCentavos=transferFee,sourceWalletId=wallet,destinationWalletId=if(mode=="Transfer")destination else null,categoryId=previewCatId,timestampEpochMs=now.toInstant().toEpochMilli(),hourOfDay=now.hour,dayOfWeek=now.dayOfWeek.value,isWeekend=now.dayOfWeek.value>=6),if(mode=="Split")Math.subtractExact(amount.value,personal) else 0L)
    }
    val hasCategory = if (mode == "Income") true else (categoryText.isNotBlank() || selectedCategory != null)
    val valid=amount.value>0&&result.isSuccess&&preview.isSuccess&&selectedWallet!=null&&(!categorised||hasCategory)&&(mode!="Split"||(n!=null&&personal>0&&debtor.isNotBlank()))&&(mode!="Transfer"||(destination!=wallet&&s.wallets.any{it.id==destination}))&&(mode!="Park"||note.isNotBlank())
    fun commit() {
        if(!valid)return
        val trimmedCat = categoryText.trim()
        val catNameParam = if (categorised && trimmedCat.isNotBlank()) trimmedCat else null
        if(mode=="Park"){
            vm.action {
                val catId = if (!catNameParam.isNullOrBlank()) {
                    val existing = s.categories.find { it.name.trim().equals(catNameParam, ignoreCase = true) }
                    existing?.id ?: vm.repository.dao.insertCategory(CategoryEntity(name = catNameParam, monthlyLimitCentavos = 0L, isDiscretionary = true, isFixedBill = false))
                } else category
                vm.repository.dao.insertImpulse(ImpulseVaultEntity(itemName = note, amountCentavos = amount.value, categoryId = catId, createdAtMs = System.currentTimeMillis()))
                vm.effects.send("Parked for seven days")
                dismiss()
            }
            return
        }
        vm.log(
            amount = personal,
            type = type,
            wallet = wallet,
            destination = if (mode == "Transfer") destination else null,
            category = if (categorised && category != 0L) category else null,
            note = note,
            fee = transferFee,
            owed = if (mode == "Split") Math.subtractExact(amount.value, personal) else 0L,
            debtor = debtor,
            capture = capture?.id,
            operationKey = operationKey,
            categoryName = if (categorised) catNameParam else null,
            onSuccess = dismiss
        )
    }
    val title=if(capture!=null)"Review payment" else when(mode){"Expense"->"Add expense";"Park"->"Planned purchase";"Opening balance"->"Opening balance";else->"Add ${mode.lowercase()}"}
    val saveLabel=if(capture!=null)"Confirm payment" else when(mode){"Expense"->"Save expense";"Park"->"Park for 7 days";"Transfer"->"Save transfer";"Split"->"Save split";"Income"->"Save income";else->"Save opening balance"}
    DetailSheet(title,{if(!s.busy)dismiss()},footer={ActionButton(saveLabel,Icons.Outlined.Check,{commit()},Modifier.fillMaxWidth(),primary=true,enabled=valid,busy=s.busy)}) {
        if(capture==null) {
            ActionRow(
                "✨ A.I Smart Parse",
                if(showAiPrompt)"Hide assistant" else "Type or paste e.g. \"Lunch 250 gcash split with Sam\"",
                Icons.Outlined.AutoAwesome,
                { showAiPrompt = !showAiPrompt }
            )
            if(showAiPrompt) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        "Describe your expense or income in natural words:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = aiPromptInput,
                        onValueChange = { aiPromptInput = it; aiError = null },
                        placeholder = { Text("e.g. Dinner 450 gcash split with Bob") },
                        singleLine = false,
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if(aiError != null) {
                        Text(aiError!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        ActionButton(
                            "Auto-Fill with A.I",
                            Icons.Outlined.AutoAwesome,
                            {
                                if(aiPromptInput.isNotBlank()) {
                                    vm.parseTransactionWithAi(
                                        prompt = aiPromptInput,
                                        onResult = { parsed ->
                                            if(parsed.amountCentavos > 0) {
                                                expression = java.math.BigDecimal.valueOf(parsed.amountCentavos, 2).toPlainString()
                                            }
                                            mode = parsed.mode
                                            if(parsed.categoryName.isNotBlank()) {
                                                categoryText = parsed.categoryName
                                                category = s.categories.find { it.name.trim().equals(parsed.categoryName.trim(), ignoreCase = true) }?.id ?: 0L
                                            }
                                            val matchedW = s.wallets.find { it.name.trim().equals(parsed.walletName.trim(), ignoreCase = true) }
                                                ?: s.wallets.find { it.name.contains(parsed.walletName, ignoreCase = true) }
                                            if(matchedW != null) {
                                                wallet = matchedW.id
                                            }
                                            if(parsed.mode == "Transfer" && parsed.destinationWalletName != null) {
                                                val matchedDest = s.wallets.find { it.name.contains(parsed.destinationWalletName, ignoreCase = true) }
                                                if(matchedDest != null) destination = matchedDest.id
                                            }
                                            if(parsed.mode == "Split") {
                                                split = (parsed.splitCount ?: 2).toString()
                                                debtor = parsed.debtorName ?: ""
                                            }
                                            if(parsed.merchantOrNote.isNotBlank()) {
                                                note = parsed.merchantOrNote
                                                noteOpen = true
                                            }
                                            showAiPrompt = false
                                            aiPromptInput = ""
                                        },
                                        onError = { err -> aiError = err }
                                    )
                                }
                            },
                            primary = true,
                            busy = isAiBusy,
                            enabled = aiPromptInput.isNotBlank() && !isAiBusy
                        )
                    }
                }
            }
        }
        OutlinedTextField(expression,{expression=it},label={Text("Amount (PHP)")},keyboardOptions=KeyboardOptions(keyboardType=if(calculator)KeyboardType.Text else KeyboardType.Decimal),isError=result.isFailure,textStyle=MaterialTheme.typography.headlineMedium.copy(fontWeight=FontWeight.Medium),singleLine=true,modifier=Modifier.fillMaxWidth())
        ErrorNotice(s.error)
        if(result.isFailure)ErrorNotice("Check the amount or calculation.")
        if(categorised) {
            OutlinedTextField(
                value = categoryText,
                onValueChange = { input ->
                    categoryText = input
                    val match = s.categories.find { it.name.trim().equals(input.trim(), ignoreCase = true) }
                    category = match?.id ?: 0L
                },
                label = { Text(if (mode == "Income") "Income category / label" else "Category / label") },
                placeholder = { Text(if (mode == "Income") "e.g. Video editing income, Housekeeping income" else "e.g. Food, Groceries, Supplies") },
                leadingIcon = { Icon(categoryIcon(categoryText), null) },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (categoryText.isNotEmpty()) {
                            IconButton(onClick = {
                                categoryText = ""
                                category = 0L
                            }) {
                                Icon(Icons.Outlined.Clear, "Clear")
                            }
                        }
                        IconButton(onClick = {
                            categorySearch = categoryText
                            picker = "category"
                        }) {
                            Icon(Icons.Outlined.ArrowDropDown, "Browse categories")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            val trimmedCat = categoryText.trim()
            val matchCat = s.categories.find { it.name.trim().equals(trimmedCat, ignoreCase = true) }
            if (trimmedCat.isNotBlank() && matchCat == null) {
                Text(
                    "New label \"$trimmedCat\" will be created and saved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
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
    if(picker.isNotEmpty())DetailSheet(when(picker){"category"->"Choose category / label";"wallet","destination"->"Choose wallet";"mode"->"Other transaction";"batch"->"Multiple expenses";"repeats"->"Repeat expense";else->"More options"},{if(!s.busy)picker=""},footer=if(picker=="batch")({ActionButton("Record batch",Icons.Outlined.Check,{vm.batch(batch,dismiss)},Modifier.fillMaxWidth(),primary=true,enabled=batchPreview.isSuccess&&batchPreview.getOrNull()?.all{it.personal.value>0}==true,busy=s.busy)}) else null) {
        when(picker) {
            "category"->{
                OutlinedTextField(
                    value = categorySearch,
                    onValueChange = { categorySearch = it },
                    label = { Text("Search or create category / label") },
                    placeholder = { Text(if (mode == "Income") "e.g. Video editing income, Housekeeping income" else "e.g. Food, Groceries, Supplies") },
                    leadingIcon = { Icon(Icons.Outlined.Search, null) },
                    trailingIcon = if (categorySearch.isNotEmpty()) {
                        { IconButton({ categorySearch = "" }) { Icon(Icons.Outlined.Clear, "Clear search") } }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                val trimmedSearch = categorySearch.trim()
                val exactMatch = s.categories.any { it.name.trim().equals(trimmedSearch, ignoreCase = true) }
                if (trimmedSearch.isNotBlank() && !exactMatch) {
                    ActionRow(
                        title = "Create \"$trimmedSearch\"",
                        subtitle = "New category / label",
                        icon = Icons.Outlined.Add,
                        onClick = {
                            categoryText = trimmedSearch
                            category = 0L
                            picker = ""
                        }
                    )
                }
                val filtered = if (trimmedSearch.isBlank()) {
                    s.categories.sortedBy { c -> s.predicted.indexOf(c.id).let { if (it < 0) 100 else it } }
                } else {
                    s.categories.filter { it.name.contains(trimmedSearch, ignoreCase = true) }
                }
                filtered.forEach { c ->
                    ActionRow(
                        title = c.name,
                        subtitle = null,
                        icon = categoryIcon(c.name),
                        onClick = {
                            categoryText = c.name
                            category = c.id
                            picker = ""
                        },
                        selected = (category == c.id) || (category == 0L && categoryText.trim().equals(c.name.trim(), ignoreCase = true))
                    )
                }
            }
            "wallet","destination"->s.wallets.filter{it.type!=WalletType.CREDIT&&(picker!="destination"||it.id!=wallet)}.forEach{w->ActionRow(w.name,Centavos(w.balanceCentavos).toPhpString(),walletIcon(w.type),{if(picker=="destination")destination=w.id else {wallet=w.id;if(destination==wallet)destination=s.wallets.firstOrNull{it.id!=wallet}?.id?:0L};picker=""},selected=if(picker=="destination")destination==w.id else wallet==w.id)}
            "mode"->listOf("Expense","Income","Transfer","Split","Park","Opening balance").forEach{m->ActionRow(if(m=="Park")"Planned purchase" else m,null,when(m){"Income"->Icons.Outlined.SouthWest;"Transfer"->Icons.Outlined.SwapHoriz;"Split"->Icons.Outlined.Group;"Park"->Icons.Outlined.Schedule;"Opening balance"->Icons.Outlined.AccountBalanceWallet;else->Icons.AutoMirrored.Outlined.ReceiptLong},{mode=m;vm.clearError();picker=""},selected=mode==m)}
            "tools"->{
                ActionRow(if(noteOpen)"Hide note" else "Add note",null,Icons.Outlined.EditNote,{noteOpen=!noteOpen;picker=""})
                ActionRow(if(calculator)"Hide calculator" else "Calculator",null,Icons.Outlined.Calculate,{calculator=!calculator;picker=""})
                ActionRow("✨ A.I Smart Parse",null,Icons.Outlined.AutoAwesome,{showAiPrompt=true;picker=""})
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
