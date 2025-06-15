package com.github.garetht.typstsupport.notifier

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project

object Notifier {
  private fun notify(project: Project, message: String, level: NotificationType) {
    val manager = NotificationGroupManager.getInstance()
    manager.getNotificationGroup(NOTIFICATION_GROUP_ID)
      .createNotification(
        message,
        level
      )
      .notify(project)
  }

  fun warn(project: Project, message: String) = notify(project, message, NotificationType.WARNING)
  fun info(project: Project, message: String) = notify(project, message, NotificationType.INFORMATION)

  private const val NOTIFICATION_GROUP_ID = "TypstSupport"
}
