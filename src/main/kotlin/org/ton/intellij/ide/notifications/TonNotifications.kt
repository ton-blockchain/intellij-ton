package org.ton.intellij.ide.notifications

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager

object TonNotifications {
    fun buildLogGroup(): NotificationGroup =
            NotificationGroupManager.getInstance().getNotificationGroup("Ton Build Log")

    fun pluginNotifications(): NotificationGroup =
            NotificationGroupManager.getInstance().getNotificationGroup("Ton Plugin")
}