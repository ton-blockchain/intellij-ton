package com.github.andreypfau.intellijton.notification

import com.intellij.openapi.components.PersistentStateComponent
import org.jdom.Element

interface LastVersionPersistence : PersistentStateComponent<Element> {
}