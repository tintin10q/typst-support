package com.github.garetht.typstsupport.notifier

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.ProjectManager

object Notifier {
  private fun notify(message: String, level: NotificationType) {
    val project = ProjectManager.getInstance().openProjects.firstOrNull()
      ?: ProjectManager.getInstance().defaultProject

    val manager = NotificationGroupManager.getInstance()
    manager.getNotificationGroup(NOTIFICATION_GROUP_ID)
      .createNotification(
        message,
        level
      )
      .notify(project)
  }

  fun warn(message: String) = notify(message, NotificationType.WARNING)
  fun info(message: String) = notify(message, NotificationType.INFORMATION)

  private const val NOTIFICATION_GROUP_ID = "TypstSupport"
}
