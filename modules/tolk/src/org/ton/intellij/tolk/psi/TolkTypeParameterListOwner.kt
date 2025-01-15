package org.ton.intellij.tolk.psi

interface TolkTypeParameterListOwner : TolkNamedElement {
    val typeParameterList: TolkTypeParameterList?
}