package org.ton.intellij.tolk.highlighting.checkers

import com.intellij.lang.annotation.AnnotationHolder
import org.ton.intellij.tolk.psi.TolkVisitor

open class TolkCheckerBase(protected val holder: AnnotationHolder) : TolkVisitor()
