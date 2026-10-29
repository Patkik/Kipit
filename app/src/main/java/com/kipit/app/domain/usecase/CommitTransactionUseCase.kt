package com.kipit.app.domain.usecase
import com.kipit.app.domain.repository.KipitRepository
import com.kipit.app.data.local.entity.TransactionEntity
import javax.inject.Inject
class CommitTransactionUseCase @Inject constructor(private val repository:KipitRepository){suspend operator fun invoke(tx:TransactionEntity,owed:Long=0,debtor:String="")=repository.commit(tx,owed,debtor)}
