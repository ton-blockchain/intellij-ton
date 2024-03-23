package org.ton.intellij.func.psi

import com.intellij.openapi.util.Key
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import org.ton.intellij.func.type.infer.FuncInferenceResult
import org.ton.intellij.func.type.infer.inferTypesIn

interface FuncInferenceContextOwner : FuncElement

private val FUNC_INFERENCE_KEY: Key<CachedValue<FuncInferenceResult>> = Key.create("FUNC_INFERENCE_KEY")

val FuncInferenceContextOwner.selfInferenceResult: FuncInferenceResult
    get() {
        return CachedValuesManager.getCachedValue(this, FUNC_INFERENCE_KEY) {
            val inferred = inferTypesIn(this)
            CachedValueProvider.Result.create(inferred, PsiModificationTracker.MODIFICATION_COUNT)
        }
    }
