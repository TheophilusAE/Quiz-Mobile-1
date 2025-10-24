package com.example.myapplication.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.myapplication.data.FirestoreRepository
import com.example.myapplication.data.Task
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class UiEvent {
    data class Success(val message: String) : UiEvent()
    data class Error(val message: String) : UiEvent()
}

class TaskViewModel(application: Application) : AndroidViewModel(application) {
    private val repo = FirestoreRepository()
    private val _events = MutableSharedFlow<UiEvent>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    // expose tasks as StateFlow for easy Compose observation
    val tasks: StateFlow<List<Task>> = repo.observeTasks()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun addTask(title: String, timestamp: Long, priority: Int = 0) {
        val t = Task(title = title, timestamp = timestamp, done = false, priority = priority)
        viewModelScope.launch {
            val res = repo.addTaskSuspend(t)
            if (res.isSuccess) {
                _events.tryEmit(UiEvent.Success("Task added"))
            } else {
                _events.tryEmit(UiEvent.Error("Add failed: ${res.exceptionOrNull()?.message}"))
            }
        }
    }

    fun toggleDone(task: Task) {
        viewModelScope.launch {
            val res = repo.updateTaskDoneSuspend(task.id, !task.done)
            if (res.isSuccess) {
                _events.tryEmit(UiEvent.Success(if (!task.done) "Marked done" else "Marked undone"))
            } else {
                _events.tryEmit(UiEvent.Error("Update failed: ${res.exceptionOrNull()?.message}"))
            }
        }
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            val res = repo.deleteTaskSuspend(taskId)
            if (res.isSuccess) {
                _events.tryEmit(UiEvent.Success("Task deleted"))
            } else {
                _events.tryEmit(UiEvent.Error("Delete failed: ${res.exceptionOrNull()?.message}"))
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            val res = repo.fetchTasksOnce()
            if (res.isSuccess) {
                _events.tryEmit(UiEvent.Success("Refreshed"))
            } else {
                _events.tryEmit(UiEvent.Error("Refresh failed: ${res.exceptionOrNull()?.message}"))
            }
        }
    }
}
