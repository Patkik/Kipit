package com.kipit.app.presentation.deeplab

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ShowChart
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.dp
import com.kipit.app.core.model.*
import com.kipit.app.presentation.*
import java.time.*
import java.time.format.DateTimeFormatter

@Composable fun DeepLabAnalyticsScreen(s:UiState) {
    var period by rememberSaveable{mutableStateOf("This month")}
    var detail by rememberSaveable{mutableStateOf("")}
    var reduction by rememberSaveable{mutableFloatStateOf(10f)}
    val today=LocalDate.now()
    val start=when(period){"Last 7 days"->today.minusDays(6);"Last 30 days"->today.minusDays(29);else->today.withDayOfMonth(1)}
    val expenses=remember(s.transactions,start,today){s.transactions.filter{it.type==TxType.EXPENSE&&it.date() in start..today}}
    val total=remember(expenses){expenses.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}
    val categories=remember(expenses,s.categories){s.categories.map{c->c to expenses.filter{it.categoryId==c.id}.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}.filter{it.second>0}.sortedByDescending{it.second}}
    val daily=remember(expenses){expenses.groupBy{it.date()}.toSortedMap()}
    AppPage {
        item{ActionRow(period,"${start.format(DateTimeFormatter.ofPattern("d MMM"))} – ${today.format(DateTimeFormatter.ofPattern("d MMM"))}",Icons.Outlined.DateRange,{detail="period"})}
        item{MoneyRow("Recorded spending","Your expenses, including fees",total)}
        item{SectionTitle("By category")}
        if(categories.isEmpty())item{EmptyState("No spending recorded","Your category breakdown appears after an expense.",Icons.Outlined.BarChart)}
        categories.forEach{(c,n)->item(key=c.id){
            MoneyRow(c.name,"${if(total>0)java.math.BigDecimal.valueOf(n).multiply(java.math.BigDecimal.valueOf(100)).divide(java.math.BigDecimal.valueOf(total),0,java.math.RoundingMode.HALF_UP).toPlainString() else "0"}% of recorded spending",n)
            LinearProgressIndicator(progress={(n.toFloat()/total.coerceAtLeast(1)).coerceIn(0f,1f)},modifier=Modifier.fillMaxWidth().height(6.dp))
        }}
        item{SectionTitle("Explore")}
        item{ActionRow("Recurring expenses",null,Icons.Outlined.Replay,{detail="patterns"})}
        item{ActionRow("Spending times",null,Icons.Outlined.Schedule,{detail="times"})}
        item{ActionRow("Daily spending",null,Icons.AutoMirrored.Outlined.ShowChart,{detail="daily"})}
        item{ActionRow("Try a budget change", "A hypothetical scenario",Icons.Outlined.Tune,{detail="scenario"})}
    }
    if(detail.isNotEmpty())DetailSheet(when(detail){"period"->"Time period";"patterns"->"Recurring expenses";"times"->"Spending times";"daily"->"Daily spending";else->"Try a budget change"},{detail=""}) {
        if(detail!="period")Text(period,style=MaterialTheme.typography.labelLarge,color=MaterialTheme.colorScheme.primary)
        when(detail) {
            "period"->listOf("This month","Last 7 days","Last 30 days").forEach{p->ActionRow(p,null,Icons.Outlined.DateRange,{period=p;detail=""},selected=period==p)}
            "patterns"->{
                val groups=expenses.filter{it.amountCentavos<=15000}.groupBy{it.categoryId to it.note}.filter{it.value.size>=2}.toList().sortedByDescending{(_,tx)->tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}
                if(groups.isEmpty())EmptyState("No recurring small expenses","Repeated expenses of ₱150 or less will appear here.",Icons.Outlined.Replay)
                groups.forEach{(key,tx)->MoneyRow(key.second.ifBlank{s.categories.find{it.id==key.first}?.name?:"Small expense"},"${tx.size} recorded payments",tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)})}
            }
            "times"->{
                val ids=s.categories.filter{it.isDiscretionary}.map{it.id}.toSet()
                val groups=expenses.filter{it.categoryId in ids}.groupBy{it.dayOfWeek to it.hourOfDay/3}.toList().sortedByDescending{(_,tx)->tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}.take(5)
                if(groups.isEmpty())EmptyState("No pattern yet","Discretionary expenses will reveal your busiest time windows.",Icons.Outlined.Schedule)
                groups.forEach{(key,tx)->MoneyRow(DayOfWeek.of(key.first).name.lowercase().replaceFirstChar{it.uppercase()},"${key.second*3}:00–${(key.second+1)*3}:00 · ${tx.size} payments",tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)})}
            }
            "daily"->{
                if(daily.isEmpty())EmptyState("No daily history","Recorded expenses will appear by date.",Icons.AutoMirrored.Outlined.ShowChart)
                else {
                    val amounts=daily.values.map{tx->tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)}}
                    SpendingChart(amounts)
                    daily.forEach{(date,tx)->MoneyRow(date.format(DateTimeFormatter.ofPattern("d MMM")),"${tx.size} recorded expenses",tx.sumMoney{Math.addExact(it.amountCentavos,it.feeCentavos)})}
                }
                Text("Days without entries are not confirmed no-spend days. A complete historical baseline is not available for comparison.",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
            }
            "scenario"->{
                Text("Reduce recorded spending by ${reduction.toInt()}%",style=MaterialTheme.typography.titleMedium)
                Slider(reduction,{reduction=it},modifier=Modifier.semantics{contentDescription="Spending reduction";stateDescription="${reduction.toInt()} percent"},valueRange=0f..50f,steps=49,enabled=total>0)
                MoneyRow("Could free up","Based on this period only",percentageCentavos(total,reduction.toInt()))
                Text("A scenario, not a forecast. It changes no records and does not estimate a savings completion date.",style=MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
@Composable private fun SpendingChart(amounts:List<Long>) {
    val ink=MaterialTheme.colorScheme.primary
    val track=MaterialTheme.colorScheme.outlineVariant
    val max=amounts.maxOrNull()?.coerceAtLeast(1)?:1L
    Text("Recorded days, in date order",style=MaterialTheme.typography.bodySmall,color=MaterialTheme.colorScheme.onSurfaceVariant)
    Canvas(Modifier.fillMaxWidth().height(120.dp).semantics{contentDescription="Spending on ${amounts.size} recorded days. Values are listed below."}) {
        drawLine(track,Offset(0f,size.height),Offset(size.width,size.height),1.dp.toPx())
        val path=Path()
        amounts.forEachIndexed{i,n->val x=if(amounts.size==1)size.width/2 else size.width*i/(amounts.size-1);val y=size.height*(1-n.toFloat()/max);if(i==0)path.moveTo(x,y) else path.lineTo(x,y);drawCircle(ink,3.dp.toPx(),Offset(x,y))}
        drawPath(path,ink,style=Stroke(2.dp.toPx()))
    }
}
