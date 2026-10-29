package com.kipit.app.domain.usecase
import com.kipit.app.data.local.dao.*
import javax.inject.Inject
data class Diagnostics(val triggers:List<TriggerWindow>,val leaks:List<MicroLeak>)
class RunBehavioralDiagnosticsUseCase @Inject constructor(private val dao:KipitDao){suspend operator fun invoke(now:Long)=Diagnostics(dao.triggers(now-2592000000L),dao.leaks(now-2592000000L))}
