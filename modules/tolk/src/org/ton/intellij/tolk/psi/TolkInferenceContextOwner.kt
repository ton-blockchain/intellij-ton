package org.ton.intellij.tolk.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.tolk.type.infer.TolkInferenceResult
import org.ton.intellij.tolk.type.infer.inferTypesIn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

interface TolkInferenceContextOwner : TolkElement

private val FUNC_INFERENCE_KEY: Key<CachedValue<TolkInferenceResult>> = Key.create("FUNC_INFERENCE_KEY")

@OptIn(ExperimentalTime::class)
val TolkInferenceContextOwner.selfInferenceResult: TolkInferenceResult
    get() {
        return CachedValuesManager.getCachedValue(this, FUNC_INFERENCE_KEY) {
            val (inferred,time) = measureTimedValue {
                inferTypesIn(this)
            }
//            println("${(this as? TolkNamedElement)?.name} - infer $time")
            CachedValueProvider.Result.create(inferred, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
