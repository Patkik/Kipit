package com.kipit.app.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.kipit.app.core.model.*
import java.time.format.DateTimeFormatter

@Composable fun ActivityScreen(s:UiState) {
    var query by rememberSaveable{mutableStateOf("")}
    var filter by rememberSaveable{mutableStateOf("All types")}
    var walletId by rememberSaveable{mutableStateOf<Long?>(null)}
    var picker by remember{mutableStateOf("")}
    var detail by rememberSaveable{mutableStateOf<Long?>(null)}
    val filtered=remember(s.transactions,s.wallets,s.categories,query,filter,walletId){s.transactions.filter { tx->
        (filter=="All types"||tx.type.label()==filter)&&(walletId==null||tx.sourceWalletId==walletId||tx.destinationWalletId==walletId)&&
        (query.isBlank()||listOf(tx.note,tx.tagsCsv,tx.amountCentavos.toString(),s.categories.find{it.id==tx.categoryId}?.name?:"",s.wallets.find{it.id==tx.sourceWalletId}?.name?:"").any{it.contains(query.trim(),true)})
    }}
    AppPage {
        item { OutlinedTextField(query,{query=it},label={Text("Search activity")},leadingIcon={Icon(Icons.Outlined.Search,null)},trailingIcon={if(query.isNotEmpty())IconButton({query=""}){Icon(Icons.Outlined.Close,"Clear search")}},singleLine=true,modifier=Modifier.fillMaxWidth()) }
        item {
            if(LocalDensity.current.fontScale>1.3f)Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                ActionButton(filter,Icons.Outlined.FilterList,{picker="type"},Modifier.fillMaxWidth())
                ActionButton(s.wallets.find{it.id==walletId}?.name?:"All wallets",Icons.Outlined.AccountBalanceWallet,{picker="wallet"},Modifier.fillMaxWidth())
            } else Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                ActionButton(filter,Icons.Outlined.FilterList,{picker="type"},Modifier.weight(1f))
                ActionButton(s.wallets.find{it.id==walletId}?.name?:"All wallets",Icons.Outlined.AccountBalanceWallet,{picker="wallet"},Modifier.weight(1f))
            }
        }
        if(filtered.isEmpty())item{EmptyState(if(s.transactions.isEmpty())"No activity yet" else "No matches",if(s.transactions.isEmpty())"Add an expense to start your history." else "Try another search or filter.",Icons.AutoMirrored.Outlined.ReceiptLong)}
        filtered.groupBy{it.date()}.forEach{(date,transactions)->
            item(key="day$date"){SectionTitle(date.format(DateTimeFormatter.ofPattern("d MMMM yyyy")))}
            items(transactions,key={it.id}){tx->LedgerRow(tx,s){detail=tx.id};HorizontalDivider(color=MaterialTheme.colorScheme.outlineVariant)}
        }
    }
    if(picker.isNotEmpty())DetailSheet(if(picker=="type")"Transaction type" else "Wallet",{picker=""}) {
        if(picker=="type") (listOf("All types")+TxType.entries.map{it.label()}).forEach{type->ActionRow(type,null,Icons.AutoMirrored.Outlined.ReceiptLong,{filter=type;picker=""},selected=filter==type)}
        else {ActionRow("All wallets",null,Icons.Outlined.AccountBalanceWallet,{walletId=null;picker=""},selected=walletId==null);s.wallets.forEach{w->ActionRow(w.name,null,walletIcon(w.type),{walletId=w.id;picker=""},selected=walletId==w.id)}}
    }
    s.transactions.find{it.id==detail}?.let{TransactionDetail(it,s){detail=null}}
}
