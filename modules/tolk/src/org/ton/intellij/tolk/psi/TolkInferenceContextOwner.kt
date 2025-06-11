package org.ton.intellij.tolk.psi

import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.psi.util.*
import org.ton.intellij.tolk.type.TolkInferenceResult
import org.ton.intellij.tolk.type.inferTypesIn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

interface TolkInferenceContextOwner : TolkElement
private val LOG = logger<TolkInferenceContextOwner>()

private val TOLK_INFERENCE_KEY: Key<CachedValue<TolkInferenceResult>> = Key.create("TOLK_INFERENCE_KEY")

@OptIn(ExperimentalTime::class)
val TolkInferenceContextOwner.selfInferenceResult: TolkInferenceResult
    get() {
        val (cachedValue, time) = measureTimedValue {
            CachedValuesManager.getCachedValue(this, TOLK_INFERENCE_KEY) {
                val (inferred, time) = measureTimedValue {
                    try {
                        inferTypesIn(this)
                    } catch (e: ProcessCanceledException) {
                        throw e
                    }
                }
//                LOG.warn("Inference cache miss: ${containingFile.name}$${toString()} in $time")
                createCachedResult(inferred)
            }
        }

        return cachedValue
    }

fun <T> TolkInferenceContextOwner.createCachedResult(value: T): CachedValueProvider.Result<T> = when {
    containingFile.virtualFile is VirtualFileWindow -> {
        CachedValueProvider.Result.create(value, PsiModificationTracker.MODIFICATION_COUNT)
    }
    else -> {
        val structureModificationTracker = project.tolkPsiManager.tolkStructureModificationCount
        val modificationTracker = PsiTreeUtil.getContextOfType(this, TolkModificationTrackerOwner::class.java, false)?.modificationTracker
        CachedValueProvider.Result.create(
            value,
            listOfNotNull(structureModificationTracker, modificationTracker)
        )
    }
}
