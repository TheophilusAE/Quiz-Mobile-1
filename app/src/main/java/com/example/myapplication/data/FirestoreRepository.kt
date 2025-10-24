package com.example.myapplication.data

import android.util.Log
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class FirestoreRepository {
    private val firestore = FirebaseFirestore.getInstance()
    private val collection = firestore.collection("tasks")
    private val TAG = "FirestoreRepository"

    fun observeTasks(): Flow<List<Task>> = callbackFlow {
        val listener = collection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                // Log the error so the developer can see why snapshots fail
                Log.e(TAG, "listen error", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val list = snapshot?.documents?.mapNotNull { doc ->
                doc.toTaskOrNull()
            } ?: emptyList()

            trySend(list)
        }

        awaitClose { listener.remove() }
    }

    fun addTask(task: Task) {
        val data = mutableMapOf<String, Any?>()
        data["title"] = task.title
        data["timestamp"] = task.timestamp
        data["done"] = task.done
        data["priority"] = task.priority
        task.completedAt?.let { data["completedAt"] = it }

        // If id is empty, let Firestore generate an id
        if (task.id.isBlank()) {
            collection.add(data)
                .addOnSuccessListener { docRef ->
                    Log.d(TAG, "task added with id=${docRef.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "failed to add task", e)
                }
        } else {
            collection.document(task.id).set(data)
                .addOnSuccessListener {
                    Log.d(TAG, "task set id=${task.id}")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "failed to set task", e)
                }
        }
    }

    // suspend versions that return Result so caller can show accurate UI feedback
    suspend fun addTaskSuspend(task: Task): Result<String> = suspendCoroutine { cont ->
        val data = mutableMapOf<String, Any?>()
        data["title"] = task.title
        data["timestamp"] = task.timestamp
        data["done"] = task.done
        data["priority"] = task.priority
        task.completedAt?.let { data["completedAt"] = it }

        collection.add(data)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "task added with id=${docRef.id}")
                cont.resume(Result.success(docRef.id))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "failed to add task", e)
                cont.resume(Result.failure(e))
            }
    }

    suspend fun updateTaskDoneSuspend(id: String, done: Boolean): Result<Unit> = suspendCoroutine { cont ->
        if (id.isBlank()) {
            cont.resume(Result.failure(IllegalArgumentException("id blank")))
            return@suspendCoroutine
        }

        val updates = if (done) {
            mapOf<String, Any>(
                "done" to true,
                "completedAt" to System.currentTimeMillis()
            )
        } else {
            mapOf<String, Any?>(
                "done" to false,
                "completedAt" to FieldValue.delete()
            )
        }

        collection.document(id).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "updated done for id=$id to $done")
                cont.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "failed to update done", e)
                cont.resume(Result.failure(e))
            }
    }

    suspend fun deleteTaskSuspend(id: String): Result<Unit> = suspendCoroutine { cont ->
        if (id.isBlank()) {
            cont.resume(Result.failure(IllegalArgumentException("id blank")))
            return@suspendCoroutine
        }

        collection.document(id).delete()
            .addOnSuccessListener {
                Log.d(TAG, "deleted id=$id")
                cont.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "failed to delete", e)
                cont.resume(Result.failure(e))
            }
    }

    suspend fun fetchTasksOnce(): Result<List<Task>> = suspendCoroutine { cont ->
        collection.get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { it.toTaskOrNull() }
                cont.resume(Result.success(list))
            }
            .addOnFailureListener { e ->
                cont.resume(Result.failure(e))
            }
    }

    fun updateTaskDone(id: String, done: Boolean) {
        if (id.isBlank()) return
        val updates = if (done) {
            mapOf<String, Any>(
                "done" to true,
                "completedAt" to System.currentTimeMillis()
            )
        } else {
            mapOf<String, Any?>(
                "done" to false,
                // remove completedAt when unchecking
                "completedAt" to FieldValue.delete()
            )
        }

        collection.document(id).update(updates)
            .addOnSuccessListener {
                Log.d(TAG, "updated done for id=$id to $done")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "failed to update done", e)
            }
    }

    private fun DocumentSnapshot.toTaskOrNull(): Task? {
        val title = getString("title") ?: return null
        val timestamp = getLong("timestamp") ?: System.currentTimeMillis()
        val done = getBoolean("done") ?: false
        val completedAt = getLong("completedAt")
        val priorityLong = getLong("priority")
        val priority = priorityLong?.toInt() ?: 0
        return Task(id = id, title = title, timestamp = timestamp, done = done, completedAt = completedAt, priority = priority)
    }
}
