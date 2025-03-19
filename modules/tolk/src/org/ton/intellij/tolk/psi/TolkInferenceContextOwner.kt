package org.ton.intellij.tolk.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.tolk.type.TolkInferenceResult
import org.ton.intellij.tolk.type.inferTypesIn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

interface TolkInferenceContextOwner : TolkElement

private val FUNC_INFERENCE_KEY: Key<CachedValue<TolkInferenceResult>> = Key.create("FUNC_INFERENCE_KEY")

@OptIn(ExperimentalTime::class)
val TolkInferenceContextOwner.selfInferenceResult: TolkInferenceResult
    get() {
        return CachedValuesManager.getCachedValue(this, FUNC_INFERENCE_KEY) {
            val (inferred, _) = measureTimedValue {
                inferTypesIn(this)
            }
            CachedValueProvider.Result.create(inferred, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
