package com.kipit.app.core.util
import com.kipit.app.core.model.Centavos
import com.kipit.app.data.local.entity.*
data class ParsedEntry(val amount:Centavos,val walletId:Long,val categoryId:Long?,val personal:Centavos,val owed:Centavos,val note:String,val tags:String)
object MicroSyntaxLexer {
 fun parse(input:String,wallets:List<WalletEntity>,categories:List<CategoryEntity>):Result<List<ParsedEntry>> = runCatching {
 input.split(',').filter{it.isNotBlank()}.map{part->
 val tokens=part.trim().split(' ').filter{it.isNotBlank()};val total=Centavos.parse(tokens.first());require(total.value>0){"Enter a positive amount"}
 var wallet=wallets.firstOrNull()?:error("Create a wallet first");var category:CategoryEntity?=null;var split=1L;val notes=mutableListOf<String>();val tags=mutableListOf<String>()
 tokens.drop(1).forEach{t->when{
 t.startsWith("#")->tags.add(t.drop(1))
 t.startsWith("split:")->{split=t.substringAfter(':').toLong();require(split in 2..100){"Split must be 2–100"}}
 wallets.any{it.name.equals(t,true) || (t.equals("cash",true)&&it.name.contains("cash",true))}->wallet=wallets.first{it.name.equals(t,true) || (t.equals("cash",true)&&it.name.contains("cash",true))}
 categories.any{it.name.equals(t,true)}->category=categories.first{it.name.equals(t,true)}
 else->notes.add(t)
 }}
 val personal=total/split;ParsedEntry(total,wallet.id,category?.id,personal,total-personal,notes.joinToString(" "),tags.joinToString(","))
 }.also{require(it.isNotEmpty()){ "Enter an expense" }}
 }
}
