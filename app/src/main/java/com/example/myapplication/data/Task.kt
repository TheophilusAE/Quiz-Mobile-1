package com.example.myapplication.data

data class Task(
    val id: String = "",
    val title: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val done: Boolean = false,
    val completedAt: Long? = null,
    val priority: Int = 0 // 0=low,1=med,2=high
)
