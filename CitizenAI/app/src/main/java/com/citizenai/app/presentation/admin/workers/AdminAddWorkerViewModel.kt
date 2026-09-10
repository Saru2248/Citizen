package com.citizenai.app.presentation.admin.workers

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.citizenai.app.data.remote.firebase.FirestoreService
import com.citizenai.app.domain.model.User
import com.citizenai.app.domain.model.UserRole
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class AdminAddWorkerUiState(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val tempPassword: String = "",
    val department: String = "Sanitation & Waste Management",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSuccess: Boolean = false
)

@HiltViewModel
class AdminAddWorkerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val firestoreService: FirestoreService
) : ViewModel() {

    private val _uiState = MutableStateFlow(AdminAddWorkerUiState())
    val uiState: StateFlow<AdminAddWorkerUiState> = _uiState.asStateFlow()

    fun onNameChange(v: String) { _uiState.value = _uiState.value.copy(name = v, error = null) }
    fun onEmailChange(v: String) { _uiState.value = _uiState.value.copy(email = v, error = null) }
    fun onPhoneChange(v: String) { _uiState.value = _uiState.value.copy(phone = v, error = null) }
    fun onPasswordChange(v: String) { _uiState.value = _uiState.value.copy(tempPassword = v, error = null) }
    fun onDepartmentChange(v: String) { _uiState.value = _uiState.value.copy(department = v) }

    fun addWorker() {
        val state = _uiState.value
        if (state.name.isBlank() || state.email.isBlank() || state.tempPassword.isBlank()) {
            _uiState.value = state.copy(error = "Please fill in all required fields (Name, Email, Password).")
            return
        }

        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            try {
                // Instantiating secondary FirebaseApp to create worker account without logging out current Admin
                val appName = "SecondaryWorkerApp_${System.currentTimeMillis()}"
                val primaryApp = FirebaseApp.getInstance()
                val secondaryApp = FirebaseApp.initializeApp(context, primaryApp.options, appName)
                val secondaryAuth = FirebaseAuth.getInstance(secondaryApp)

                val authResult = secondaryAuth.createUserWithEmailAndPassword(
                    state.email.trim(),
                    state.tempPassword.trim()
                ).await()

                val workerUid = authResult.user?.uid ?: throw Exception("Failed to create worker account in Firebase Auth")
                secondaryAuth.signOut()
                secondaryApp.delete()

                val workerUser = User(
                    id = workerUid,
                    name = state.name.trim(),
                    email = state.email.trim(),
                    phone = state.phone.trim(),
                    role = UserRole.WORKER,
                    department = state.department,
                    workerId = "WRK-${(1000..9999).random()}"
                )

                val saveResult = firestoreService.saveUser(workerUser)
                if (saveResult.isSuccess) {
                    _uiState.value = _uiState.value.copy(isLoading = false, isSuccess = true)
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = saveResult.exceptionOrNull()?.message ?: "Failed to save worker profile to Firestore."
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.message ?: "Failed to create worker account."
                )
            }
        }
    }
}
