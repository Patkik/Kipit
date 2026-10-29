package com.kipit.app.data.local
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.*
/** Isolated sample build only. Never installed into the personal ledger. */
class SampleSeed:RoomDatabase.Callback(){override fun onCreate(db:SupportSQLiteDatabase){
 val now=ZonedDateTime.now();val categoryNames=listOf("Food","Transport","Utilities","Tech & Gaming","Groceries","Subscriptions")
 db.execSQL("INSERT INTO user_config(id,mealAnchorCentavos,savingsGoalCentavos,lockedSavingsCentavos,currentNoSpendStreak,impulseSavedCentavos,lastSweepDate,lastStreakDate) VALUES(1,7500,1000000,120000,0,0,'','')")
 listOf("Physical Cash","GCash","Maya","BPI Savings").forEachIndexed{i,name->db.execSQL("INSERT INTO wallets(id,name,type,balanceCentavos,isArchived) VALUES(?,?,?,?,0)",arrayOf(i+1,name,if(i==0)"CASH" else if(i==3)"BANK" else "E_WALLET",if(i==3)1500000 else 600000))}
 categoryNames.forEachIndexed{i,name->db.execSQL("INSERT INTO categories(id,name,iconName,colorHex,monthlyLimitCentavos,isDiscretionary,isFixedBill) VALUES(?,?,'circle',4282542939,?,?,?)",arrayOf(i+1,name,if(i==2)300000 else 150000,if(i==2)0 else 1,if(i==2)1 else 0))}
 var id=0L
 for(ago in 29L downTo 0L){val day=now.minusDays(ago).withHour(12).withMinute(0);val weekend=day.dayOfWeek.value>=6
 for(kind in 0..1){id++;val amount=if(kind==0) (if(weekend)18000L else 12000L) else 3000L;val wallet=if(kind==0)2 else 1;val note=if(kind==0)"Lunch" else "Commute";val category=if(kind==0)1 else 2
 db.execSQL("INSERT INTO transactions(id,type,amountCentavos,feeCentavos,sourceWalletId,destinationWalletId,categoryId,timestampEpochMs,hourOfDay,dayOfWeek,isWeekend,note,tagsCsv,operationKey) VALUES(?,'EXPENSE',?,0,?,NULL,?,?,?,?,?,?,'sample',?)",arrayOf(id,amount,wallet,category,day.toInstant().toEpochMilli(),if(kind==0)12 else 8,day.dayOfWeek.value,if(weekend)1 else 0,note,"sample:$id"))
 db.execSQL("INSERT INTO tag_fts(rowid,note,tagsCsv) VALUES(?,?,'sample')",arrayOf(id,note));db.execSQL("UPDATE wallets SET balanceCentavos=balanceCentavos-? WHERE id=?",arrayOf(amount,wallet))
 }}
}}
