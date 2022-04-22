package org.ton.intellij.notification

import com.intellij.openapi.components.PersistentStateComponent
import org.jdom.Element

interface LastVersionPersistence : PersistentStateComponent<Element> {
}