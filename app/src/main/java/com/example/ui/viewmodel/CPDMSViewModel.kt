package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.CPDMSRepository
import com.example.data.dao.ProjectMemberWithUser
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

class CPDMSViewModel(val repository: CPDMSRepository) : ViewModel() {

    // Authentication States
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _company = MutableStateFlow<Company?>(null)
    val company: StateFlow<Company?> = _company.asStateFlow()

    // Project & Work States
    private val _currentProject = MutableStateFlow<Project?>(null)
    val currentProject: StateFlow<Project?> = _currentProject.asStateFlow()

    private val _currentStage = MutableStateFlow<Stage?>(null)
    val currentStage: StateFlow<Stage?> = _currentStage.asStateFlow()

    private val _currentWorkItem = MutableStateFlow<WorkItem?>(null)
    val currentWorkItem: StateFlow<WorkItem?> = _currentWorkItem.asStateFlow()

    // Dynamic Lists (collected reactively or updated on demand)
    val allPlans: StateFlow<List<SubscriptionPlan>> = repository.subscriptionDao.getAllPlans()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allCompanies: StateFlow<List<Company>> = repository.companyDao.getAllCompanies()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.userDao.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Projects based on user role
    val projects: StateFlow<List<Project>> = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else if (user.role == "Engineering Office" && user.companyId != null) {
            repository.projectDao.getProjectsByCompany(user.companyId)
        } else {
            repository.projectDao.getProjectsByMember(user.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Active project members
    val projectMembers: StateFlow<List<ProjectMemberWithUser>> = _currentProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList())
        else repository.projectDao.getMembersWithUserByProject(proj.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Invitation codes for current project
    val invitationCodes: StateFlow<List<InvitationCode>> = _currentProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList())
        else repository.invitationCodeDao.getCodesByProject(proj.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Stages of current project
    val stages: StateFlow<List<Stage>> = _currentProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList())
        else repository.stageDao.getStagesByProject(proj.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Work items of current stage
    val workItems: StateFlow<List<WorkItem>> = _currentStage.flatMapLatest { stage ->
        if (stage == null) flowOf(emptyList())
        else repository.workItemDao.getItemsByStage(stage.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Selected item approvals
    val approvals: StateFlow<List<Approval>> = _currentWorkItem.flatMapLatest { item ->
        if (item == null) flowOf(emptyList())
        else repository.approvalDao.getApprovalsByItem(item.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Comments for work items
    val comments: StateFlow<List<Comment>> = _currentWorkItem.flatMapLatest { item ->
        if (item == null) {
            _currentProject.flatMapLatest { proj ->
                if (proj == null) flowOf(emptyList())
                else repository.commentDao.getCommentsByProject(proj.id)
            }
        } else {
            repository.commentDao.getCommentsByItem(item.id)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Site observations of current project
    val siteObservations: StateFlow<List<SiteObservation>> = _currentProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList())
        else repository.siteObservationDao.getObservationsByProject(proj.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Notifications of current user
    val notifications: StateFlow<List<Notification>> = _currentUser.flatMapLatest { user ->
        if (user == null) flowOf(emptyList())
        else repository.notificationDao.getNotificationsByUser(user.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Files of current project
    val files: StateFlow<List<ProjectFile>> = _currentProject.flatMapLatest { proj ->
        if (proj == null) flowOf(emptyList())
        else repository.fileDao.getFilesByProject(proj.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Subscription status for Engineering Office
    val currentSubscription: StateFlow<Subscription?> = _company.flatMapLatest { comp ->
        if (comp == null) flowOf(null)
        else repository.subscriptionDao.getSubscriptionByCompany(comp.id)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    init {
        viewModelScope.launch {
            repository.prepopulateIfNeeded()
        }
    }

    // Methods
    fun selectProject(project: Project?) {
        _currentProject.value = project
        _currentStage.value = null
        _currentWorkItem.value = null
    }

    fun selectStage(stage: Stage?) {
        _currentStage.value = stage
        _currentWorkItem.value = null
    }

    fun selectWorkItem(item: WorkItem?) {
        _currentWorkItem.value = item
    }

    fun login(emailOrPhone: String, passwordHash: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = if (emailOrPhone.contains("@")) {
                repository.userDao.getUserByEmail(emailOrPhone.trim())
            } else {
                repository.userDao.getUserByPhone(emailOrPhone.trim())
            }

            if (user == null) {
                onError("Account not found")
            } else if (user.passwordHash != passwordHash) {
                onError("Incorrect password")
            } else if (user.accountStatus == "Suspended") {
                onError("Your account has been suspended by the administrator")
            } else {
                _currentUser.value = user
                if (user.companyId != null) {
                    repository.companyDao.getCompanyById(user.companyId).firstOrNull()?.let {
                        _company.value = it
                    }
                } else {
                    _company.value = null
                }
                onSuccess()
            }
        }
    }

    fun register(
        firstName: String,
        lastName: String,
        email: String,
        phone: String,
        passwordHash: String,
        companyName: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            if (repository.userDao.getUserByEmail(email.trim()) != null) {
                onError("Email address already registered")
                return@launch
            }

            val userId = UUID.randomUUID().toString()
            var companyId: String? = null

            // If registering as Engineering Office, create a Company Profile automatically
            if (!companyName.isNullOrBlank()) {
                companyId = UUID.randomUUID().toString()
                val company = Company(
                    id = companyId,
                    name = companyName,
                    country = "Saudi Arabia",
                    city = "Riyadh",
                    address = "Default Office address",
                    phone = phone,
                    email = email,
                    status = "Active",
                    subscriptionPlanId = "starter_id" // Start with Starter plan
                )
                repository.companyDao.insertCompany(company)

                // Add active starter subscription
                repository.subscriptionDao.insertSubscription(
                    Subscription(
                        id = UUID.randomUUID().toString(),
                        companyId = companyId,
                        planId = "starter_id",
                        startDate = System.currentTimeMillis(),
                        endDate = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
                        status = "Active",
                        paymentStatus = "Paid"
                    )
                )
                _company.value = company
            }

            val newUser = User(
                id = userId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                phoneNumber = phone,
                passwordHash = passwordHash,
                role = if (companyName.isNullOrBlank()) "Owner" else "Engineering Office", // Default to Owner unless they specify company details
                accountStatus = "Active",
                emailVerified = true,
                createdDate = System.currentTimeMillis(),
                companyId = companyId
            )

            repository.userDao.insertUser(newUser)
            _currentUser.value = newUser
            onSuccess()
        }
    }

    fun joinProject(codeString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val user = _currentUser.value
            if (user == null) {
                onError("Please log in first")
                return@launch
            }
            val codeObj = repository.invitationCodeDao.getCodeByString(codeString.trim().uppercase())

            if (codeObj == null) {
                onError("Invalid access code")
                return@launch
            }

            if (codeObj.status != "Active") {
                onError("This invitation code is ${codeObj.status.lowercase()}")
                return@launch
            }

            if (codeObj.expiryDate < System.currentTimeMillis()) {
                onError("This invitation code has expired")
                return@launch
            }

            // Bind user to the project
            val memberId = UUID.randomUUID().toString()
            val newMember = ProjectMember(
                id = memberId,
                projectId = codeObj.projectId,
                userId = user.id,
                role = codeObj.userType,
                contractorType = codeObj.contractorType,
                joinDate = System.currentTimeMillis(),
                permissionSet = "VIEW,SUBMIT"
            )
            repository.projectDao.insertMember(newMember)

            // Update user's role to match the invitation code type
            val updatedUser = user.copy(role = codeObj.userType)
            repository.userDao.insertUser(updatedUser)
            _currentUser.value = updatedUser

            // Update code status
            repository.invitationCodeDao.updateCode(
                codeObj.copy(status = "Used", usedBy = user.id)
            )

            // Select project immediately
            val project = repository.projectDao.getProjectById(codeObj.projectId).firstOrNull()
            _currentProject.value = project

            // Create notification for Engineering Office
            repository.notificationDao.insertNotification(
                Notification(
                    id = UUID.randomUUID().toString(),
                    userId = codeObj.createdBy,
                    title = "New Member Joined",
                    message = "${user.firstName} joined ${project?.name ?: "your project"} as ${codeObj.userType}.",
                    type = "invitation",
                    readStatus = false,
                    createdDate = System.currentTimeMillis()
                )
            )

            onSuccess()
        }
    }

    fun logout() {
        _currentUser.value = null
        _company.value = null
        _currentProject.value = null
        _currentStage.value = null
        _currentWorkItem.value = null
    }

    // Easy account switcher helper for effortless evaluation
    fun quickSwitchAccount(role: String) {
        viewModelScope.launch {
            val email = when (role) {
                "Super Admin" -> "admin@cpdms.com"
                "Engineering Office" -> "eo@cpdms.com"
                "Owner" -> "owner@cpdms.com"
                "Contractor" -> "contractor@cpdms.com"
                "Resident Engineer" -> "engineer@cpdms.com"
                else -> return@launch
            }
            val user = repository.userDao.getUserByEmail(email) ?: return@launch
            _currentUser.value = user
            if (user.companyId != null) {
                _company.value = repository.companyDao.getCompanyById(user.companyId).firstOrNull()
            } else {
                _company.value = null
            }
            // Auto-select the default project
            val proj = repository.projectDao.getProjectById("proj_apex_1").firstOrNull()
            selectProject(proj)
        }
    }

    // Project Admin Operations
    fun createProject(name: String, description: String, location: String, type: String, startDate: Long, expectedEndDate: Long) {
        val user = _currentUser.value ?: return
        val compId = user.companyId ?: return
        viewModelScope.launch {
            val projId = UUID.randomUUID().toString()
            val newProj = Project(
                id = projId,
                companyId = compId,
                name = name,
                description = description,
                location = location,
                projectType = type,
                startDate = startDate,
                expectedEndDate = expectedEndDate,
                status = "Active",
                createdBy = user.id
            )
            repository.projectDao.insertProject(newProj)

            // Add creator as member
            repository.projectDao.insertMember(
                ProjectMember(
                    id = UUID.randomUUID().toString(),
                    projectId = projId,
                    userId = user.id,
                    role = "Engineering Office",
                    joinDate = System.currentTimeMillis(),
                    permissionSet = "ALL"
                )
            )

            selectProject(newProj)
        }
    }

    fun updateProjectStatus(status: String) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val updated = proj.copy(status = status)
            repository.projectDao.updateProject(updated)
            _currentProject.value = updated
        }
    }

    fun generateInvitationCode(roleType: String, contractorType: String? = null): String {
        val proj = _currentProject.value ?: return ""
        val user = _currentUser.value ?: return ""
        val codePrefix = when (roleType) {
            "Owner" -> "OWN-"
            "Resident Engineer" -> "ENG-"
            else -> "CON-"
        }
        val randomDigits = (100000..999999).random().toString()
        val generated = "$codePrefix$randomDigits"

        viewModelScope.launch {
            repository.invitationCodeDao.insertCode(
                InvitationCode(
                    id = UUID.randomUUID().toString(),
                    code = generated,
                    projectId = proj.id,
                    userType = roleType,
                    contractorType = contractorType,
                    createdBy = user.id,
                    expiryDate = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                    status = "Active"
                )
            )
        }
        return generated
    }

    fun updateCompanyProfile(name: String, country: String, city: String, address: String, phone: String, email: String, reg: String?, tax: String?) {
        val comp = _company.value ?: return
        viewModelScope.launch {
            val updated = comp.copy(
                name = name,
                country = country,
                city = city,
                address = address,
                phone = phone,
                email = email,
                registrationNumber = reg,
                taxNumber = tax
            )
            repository.companyDao.insertCompany(updated)
            _company.value = updated
        }
    }

    // Subscription actions
    fun upgradeSubscription(planId: String) {
        val comp = _company.value ?: return
        viewModelScope.launch {
            repository.subscriptionDao.insertSubscription(
                Subscription(
                    id = UUID.randomUUID().toString(),
                    companyId = comp.id,
                    planId = planId,
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
                    status = "Active",
                    paymentStatus = "Paid"
                )
            )
            val updatedComp = comp.copy(subscriptionPlanId = planId)
            repository.companyDao.insertCompany(updatedComp)
            _company.value = updatedComp
        }
    }

    // Stage Operations
    fun createStage(name: String, description: String, startDate: Long, endDate: Long, contractorId: String?, engineerId: String?) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val stageId = UUID.randomUUID().toString()
            val newStage = Stage(
                id = stageId,
                projectId = proj.id,
                name = name,
                description = description,
                startDate = startDate,
                endDate = endDate,
                status = "Not Started",
                assignedContractorId = contractorId,
                assignedEngineerId = engineerId
            )
            repository.stageDao.insertStage(newStage)
        }
    }

    fun updateStageStatus(stage: Stage, status: String) {
        viewModelScope.launch {
            val updated = stage.copy(status = status)
            repository.stageDao.updateStage(updated)
            if (_currentStage.value?.id == stage.id) {
                _currentStage.value = updated
            }
        }
    }

    // Work Item Operations
    fun createWorkItem(name: String, description: String, dueDate: Long, contractorId: String?, engineerId: String?) {
        val stage = _currentStage.value ?: return
        viewModelScope.launch {
            val itemId = UUID.randomUUID().toString()
            val newItem = WorkItem(
                id = itemId,
                stageId = stage.id,
                contractorId = contractorId,
                engineerId = engineerId,
                name = name,
                description = description,
                dueDate = dueDate,
                status = "In Progress",
                completionPercentage = 0
            )
            repository.workItemDao.insertItem(newItem)

            // Notify Contractor if assigned
            if (contractorId != null) {
                repository.notificationDao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = contractorId,
                        title = "New Task Assigned",
                        message = "You have been assigned to task '$name' under stage '${stage.name}'.",
                        type = "assignment",
                        readStatus = false,
                        createdDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateWorkItemProgress(percentage: Int) {
        val item = _currentWorkItem.value ?: return
        viewModelScope.launch {
            val updated = item.copy(completionPercentage = percentage)
            repository.workItemDao.updateItem(updated)
            _currentWorkItem.value = updated
        }
    }

    // Submit Completed Work (Contractor)
    fun submitWorkItem(notes: String, attachments: List<String>) {
        val item = _currentWorkItem.value ?: return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val updated = item.copy(status = "Submitted For Review", completionPercentage = 100)
            repository.workItemDao.updateItem(updated)
            _currentWorkItem.value = updated

            // Create Approval History entry
            repository.approvalDao.insertApproval(
                Approval(
                    id = UUID.randomUUID().toString(),
                    itemId = item.id,
                    submittedBy = user.id,
                    reviewedBy = null,
                    status = "Submitted",
                    comment = notes,
                    date = System.currentTimeMillis()
                )
            )

            // Upload files mock simulation
            attachments.forEach { file ->
                repository.fileDao.insertFile(
                    ProjectFile(
                        id = UUID.randomUUID().toString(),
                        projectId = _currentProject.value?.id ?: "",
                        stageId = _currentStage.value?.id,
                        itemId = item.id,
                        uploadedBy = user.id,
                        fileName = file,
                        filePath = "simulated/files/$file",
                        fileType = "image",
                        uploadDate = System.currentTimeMillis()
                    )
                )
            }

            // Create comment
            repository.commentDao.insertComment(
                Comment(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = "${user.firstName} (Contractor)",
                    userRole = "Contractor",
                    projectId = _currentProject.value?.id ?: "",
                    itemId = item.id,
                    parentId = null,
                    commentText = "Submitted for Review. Notes: $notes",
                    createdDate = System.currentTimeMillis()
                )
            )

            // Notify Resident Engineer if assigned
            if (item.engineerId != null) {
                repository.notificationDao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = item.engineerId,
                        title = "Work Submitted for Review",
                        message = "Contractor ${user.firstName} submitted task '${item.name}' for inspection.",
                        type = "approval",
                        readStatus = false,
                        createdDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Review Actions (Resident Engineer)
    fun reviewWorkItem(action: String, comment: String) { // "Approve", "Reject", "Request Correction"
        val item = _currentWorkItem.value ?: return
        val user = _currentUser.value ?: return
        viewModelScope.launch {
            val statusMap = mapOf(
                "Approve" to "Approved",
                "Reject" to "Rejected",
                "Request Correction" to "Needs Correction"
            )
            val newStatus = statusMap[action] ?: return@launch

            val updated = item.copy(status = newStatus)
            repository.workItemDao.updateItem(updated)
            _currentWorkItem.value = updated

            // Log Approval entry
            repository.approvalDao.insertApproval(
                Approval(
                    id = UUID.randomUUID().toString(),
                    itemId = item.id,
                    submittedBy = item.contractorId ?: "",
                    reviewedBy = user.id,
                    status = newStatus,
                    comment = comment,
                    date = System.currentTimeMillis()
                )
            )

            // Create comment
            repository.commentDao.insertComment(
                Comment(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = "${user.firstName} (Resident Engineer)",
                    userRole = "Resident Engineer",
                    projectId = _currentProject.value?.id ?: "",
                    itemId = item.id,
                    parentId = null,
                    commentText = "Status changed to $newStatus. Feedback: $comment",
                    createdDate = System.currentTimeMillis()
                )
            )

            // Notify Contractor if assigned
            if (item.contractorId != null) {
                repository.notificationDao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = item.contractorId,
                        title = "Review Status: $newStatus",
                        message = "Engineer ${user.firstName} marked task '${item.name}' as $newStatus.",
                        type = "approval",
                        readStatus = false,
                        createdDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    // Comments & Communications
    fun addComment(text: String) {
        val user = _currentUser.value ?: return
        val proj = _currentProject.value ?: return
        val item = _currentWorkItem.value
        viewModelScope.launch {
            repository.commentDao.insertComment(
                Comment(
                    id = UUID.randomUUID().toString(),
                    userId = user.id,
                    userName = "${user.firstName} (${user.role})",
                    userRole = user.role,
                    projectId = proj.id,
                    itemId = item?.id,
                    parentId = null,
                    commentText = text,
                    createdDate = System.currentTimeMillis()
                )
            )
        }
    }

    // Site Observation Operations
    fun createSiteObservation(title: String, description: String, location: String, stageId: String?, contractorId: String?, photoName: String?) {
        val proj = _currentProject.value ?: return
        viewModelScope.launch {
            val observation = SiteObservation(
                id = UUID.randomUUID().toString(),
                projectId = proj.id,
                title = title,
                description = description,
                location = location,
                relatedStageId = stageId,
                relatedContractorId = contractorId,
                photoPath = photoName?.let { "simulated/photos/$it" },
                date = System.currentTimeMillis(),
                status = "Open"
            )
            repository.siteObservationDao.insertObservation(observation)

            // If a contractor is related, notify them
            if (contractorId != null) {
                repository.notificationDao.insertNotification(
                    Notification(
                        id = UUID.randomUUID().toString(),
                        userId = contractorId,
                        title = "New Site Observation Recorded",
                        message = "Engineer recorded an issue: '$title' at location '$location'.",
                        type = "assignment",
                        readStatus = false,
                        createdDate = System.currentTimeMillis()
                    )
                )
            }
        }
    }

    fun updateObservationStatus(obs: SiteObservation, status: String) {
        viewModelScope.launch {
            repository.siteObservationDao.insertObservation(
                obs.copy(status = status)
            )
        }
    }

    // Notification mark as read
    fun markNotificationRead(id: String) {
        viewModelScope.launch {
            repository.notificationDao.markNotificationAsRead(id)
        }
    }

    // Super Admin operations
    fun createSubscriptionPlan(name: String, price: Double, duration: Int, projectsLimit: Int, ownersLimit: Int, contractorsLimit: Int, engineersLimit: Int, storageLimit: Int) {
        viewModelScope.launch {
            repository.subscriptionDao.insertPlan(
                SubscriptionPlan(
                    id = UUID.randomUUID().toString(),
                    name = name,
                    price = price,
                    durationDays = duration,
                    maxProjects = projectsLimit,
                    maxOwners = ownersLimit,
                    maxContractors = contractorsLimit,
                    maxEngineers = engineersLimit,
                    storageLimitGb = storageLimit,
                    status = "Active"
                )
            )
        }
    }

    fun toggleCompanyStatus(company: Company) {
        viewModelScope.launch {
            val newStatus = if (company.status == "Active") "Suspended" else "Active"
            repository.companyDao.insertCompany(company.copy(status = newStatus))
        }
    }

    fun toggleUserStatus(user: User) {
        viewModelScope.launch {
            val newStatus = if (user.accountStatus == "Active") "Suspended" else "Active"
            repository.userDao.insertUser(user.copy(accountStatus = newStatus))
        }
    }
}
