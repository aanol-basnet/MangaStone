package com.personal.mangarock.ui.screens.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.personal.mangarock.data.local.dao.NotificationDao
import com.personal.mangarock.data.local.entities.NotificationEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val notificationDao: NotificationDao
) : ViewModel() {

    val notifications: StateFlow<List<NotificationEntity>> = notificationDao.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun markAsRead(id: Int) = viewModelScope.launch {
        notificationDao.markAsRead(id)
    }

    fun markAllAsRead() = viewModelScope.launch {
        notificationDao.markAllAsRead()
    }

    fun clearAll() = viewModelScope.launch {
        notificationDao.deleteAll()
    }
}
