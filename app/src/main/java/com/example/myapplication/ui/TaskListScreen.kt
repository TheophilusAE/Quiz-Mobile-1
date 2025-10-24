package com.example.myapplication.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.launch
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.DismissValue
import androidx.compose.material.DismissDirection
import androidx.compose.material.rememberDismissState
import androidx.compose.material.icons.filled.Delete
import kotlinx.coroutines.delay
import com.google.accompanist.swiperefresh.SwipeRefresh
import com.google.accompanist.swiperefresh.rememberSwipeRefreshState
import androidx.compose.material.DismissState
// removed viewModel() default import; view model will be provided by the caller
import com.example.myapplication.data.Task
import com.example.myapplication.viewmodel.TaskViewModel
// using rememberCoroutineScope and calling scope.launch() (no top-level import needed)
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.material.ExperimentalMaterialApi::class)
@Composable
fun TaskListScreen(viewModel: TaskViewModel) {
    val tasks by viewModel.tasks.collectAsState()
    val showDialog = remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("My Tasks") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { showDialog.value = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add")
            }
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        // collect events from ViewModel and show snackbars on actual result
        LaunchedEffect(viewModel) {
            viewModel.events.collect { ev ->
                when (ev) {
                    is com.example.myapplication.viewmodel.UiEvent.Success -> snackbarHostState.showSnackbar(ev.message)
                    is com.example.myapplication.viewmodel.UiEvent.Error -> snackbarHostState.showSnackbar(ev.message)
                }
            }
        }

        var isRefreshing by remember { mutableStateOf(false) }
        var confirmDeleteFor by remember { mutableStateOf<Task?>(null) }

        Surface(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {

            SwipeRefresh(state = rememberSwipeRefreshState(isRefreshing), onRefresh = {
                scope.launch {
                    isRefreshing = true
                    viewModel.refresh()
                    // viewModel will emit events; small delay to show refresh spinner briefly
                    kotlinx.coroutines.delay(400)
                    isRefreshing = false
                }
            }) {

                if (tasks.isEmpty()) {
                    // empty state
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(text = "No tasks yet. Tap + to add one.", style = MaterialTheme.typography.bodyLarge)
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(tasks, key = { it.id }) { task ->
                            val dismissState = rememberDismissState(confirmStateChange = { newValue ->
                                if (newValue == DismissValue.DismissedToEnd || newValue == DismissValue.DismissedToStart) {
                                    // request confirmation; don't auto-dismiss
                                    confirmDeleteFor = task
                                    false
                                } else true
                            })

                            SwipeToDismiss(state = dismissState, directions = setOf(DismissDirection.EndToStart, DismissDirection.StartToEnd),
                                background = {
                                    // simple red background when swiping
                                    Box(modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp), contentAlignment = Alignment.CenterEnd) {
                                        androidx.compose.material.Icon(Icons.Default.Delete, contentDescription = "Delete")
                                    }
                                }) {
                                Card(modifier = Modifier.fillMaxWidth()) {
                                    TaskRow(task = task, onToggle = {
                                        viewModel.toggleDone(task)
                                    })
                                }
                            }
                        }
                    }
                }

            }

            if (showDialog.value) {
                AddTaskDialog(
                    onCancel = { showDialog.value = false },
                    onAdd = { title, timestamp, priority ->
                        viewModel.addTask(title, timestamp, priority)
                        showDialog.value = false
                    }
                )
            }

            // confirm delete dialog
            confirmDeleteFor?.let { toDelete ->
                androidx.compose.material.AlertDialog(
                    onDismissRequest = { confirmDeleteFor = null },
                    title = { Text("Delete task") },
                    text = { Text("Are you sure you want to delete '${toDelete.title}'?") },
                    confirmButton = {
                        Button(onClick = {
                            viewModel.deleteTask(toDelete.id)
                            confirmDeleteFor = null
                        }) { Text("Delete") }
                    },
                    dismissButton = { Button(onClick = { confirmDeleteFor = null }) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
fun TaskRow(task: Task, onToggle: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy - HH:mm", Locale.getDefault()) }
        Row(modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            // avatar initial
            val initial = task.title.firstOrNull()?.uppercaseChar()?.toString() ?: "T"
            androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.CircleShape, modifier = Modifier.size(40.dp), color = MaterialTheme.colorScheme.primary) {
                Box(contentAlignment = Alignment.Center) {
                    Text(text = initial, color = MaterialTheme.colorScheme.onPrimary)
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.fillMaxWidth(0.55f)) {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                Text(text = dateFormat.format(task.timestamp), style = MaterialTheme.typography.bodySmall)
                task.completedAt?.let { completed ->
                    Text(text = "Completed: ${dateFormat.format(completed)}", style = MaterialTheme.typography.bodySmall)
                }
            }

            // priority indicator
            val priorityText = when (task.priority) { 2 -> "High"; 1 -> "Med"; else -> "Low" }
            androidx.compose.material3.Surface(shape = MaterialTheme.shapes.small, color = if (task.priority==2) androidx.compose.ui.graphics.Color(0xFFFFCDD2) else androidx.compose.ui.graphics.Color(0xFFE3F2FD)) {
                Text(text = priorityText, modifier = Modifier.padding(8.dp))
            }

            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(
                checked = task.done,
                onCheckedChange = { onToggle() },
                modifier = Modifier.align(Alignment.CenterVertically)
            )
        }
}

@Composable
fun AddTaskDialog(onCancel: () -> Unit, onAdd: (String, Long, Int) -> Unit) {
    val context = LocalContext.current

    val titleState = remember { mutableStateOf("") }
    val timeState = remember { mutableStateOf(Calendar.getInstance()) }
    val priorityState = remember { mutableStateOf(0) }

    fun pickDateTime() {
        val cal = Calendar.getInstance()
        DatePickerDialog(context, { _, y, m, d ->
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m)
            cal.set(Calendar.DAY_OF_MONTH, d)
            TimePickerDialog(context, { _, hour, minute ->
                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, minute)
                timeState.value = cal
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show()
        }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
    }

    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Add Task") },
        text = {
            Column {
                OutlinedTextField(value = titleState.value, onValueChange = { titleState.value = it }, label = { Text("Title") })
                Button(onClick = { pickDateTime() }, modifier = Modifier.padding(top = 8.dp)) {
                    Text("Pick date & time")
                }
                Text(text = "Selected: ${timeState.value.time}", modifier = Modifier.padding(top = 8.dp))

                // Priority selector
                Spacer(modifier = Modifier.size(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { priorityState.value = 0 }, colors = if (priorityState.value==0) androidx.compose.material3.ButtonDefaults.buttonColors() else androidx.compose.material3.ButtonDefaults.buttonColors()) { Text("Low") }
                    Button(onClick = { priorityState.value = 1 }) { Text("Med") }
                    Button(onClick = { priorityState.value = 2 }) { Text("High") }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val title = titleState.value.trim()
                if (title.isNotEmpty()) {
                    onAdd(title, timeState.value.timeInMillis, priorityState.value)
                }
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onCancel) { Text("Cancel") }
        }
    )
}
