package org.ton.intellij.tolk.psi.impl

import org.ton.intellij.tolk.psi.TolkSelfParameter
import org.ton.intellij.util.greenStub

val TolkSelfParameter.isMutable get() = greenStub?.isMutable ?: (mutateKeyword != null)
