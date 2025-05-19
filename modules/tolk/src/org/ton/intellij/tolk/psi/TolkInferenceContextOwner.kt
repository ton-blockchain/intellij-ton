package org.ton.intellij.tolk.psi

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.ton.intellij.tolk.type.TolkInferenceResult
import org.ton.intellij.tolk.type.inferTypesIn
import kotlin.time.ExperimentalTime
import kotlin.time.measureTimedValue

interface TolkInferenceContextOwner : TolkElement

private val TOLK_INFERENCE_KEY: Key<CachedValue<TolkInferenceResult>> = Key.create("TOLK_INFERENCE_KEY")

@OptIn(ExperimentalTime::class)
val TolkInferenceContextOwner.selfInferenceResult: TolkInferenceResult
    get() {
        return CachedValuesManager.getCachedValue(this, TOLK_INFERENCE_KEY) {
            val (inferred, time) = measureTimedValue {
                try {
                    inferTypesIn(this)
                } catch (e: ProcessCanceledException) {
                    throw e
                }
            }
            CachedValueProvider.Result.create(inferred, this)
        }
    }
