package com.example.data.repository

import com.example.data.database.CPDMSDatabase
import com.example.data.model.*
import com.example.data.dao.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.util.UUID

class CPDMSRepository(private val db: CPDMSDatabase) {

    val userDao = db.userDao()
    val companyDao = db.companyDao()
    val subscriptionDao = db.subscriptionDao()
    val projectDao = db.projectDao()
    val invitationCodeDao = db.invitationCodeDao()
    val stageDao = db.stageDao()
    val workItemDao = db.workItemDao()
    val approvalDao = db.approvalDao()
    val fileDao = db.fileDao()
    val commentDao = db.commentDao()
    val notificationDao = db.notificationDao()
    val siteObservationDao = db.siteObservationDao()

    // Prepopulate subscription plans and a Super Admin user if not exists
    suspend fun prepopulateIfNeeded() {
        val plans = subscriptionDao.getAllPlans().firstOrNull()
        if (plans.isNullOrEmpty()) {
            // Add Plans
            subscriptionDao.insertPlan(
                SubscriptionPlan(
                    id = "starter_id",
                    name = "Starter Plan",
                    price = 49.0,
                    durationDays = 30,
                    maxProjects = 3,
                    maxOwners = 2,
                    maxContractors = 5,
                    maxEngineers = 2,
                    storageLimitGb = 10,
                    status = "Active"
                )
            )
            subscriptionDao.insertPlan(
                SubscriptionPlan(
                    id = "pro_id",
                    name = "Professional Plan",
                    price = 149.0,
                    durationDays = 30,
                    maxProjects = 10,
                    maxOwners = 2,
                    maxContractors = 5,
                    maxEngineers = 2,
                    storageLimitGb = 50,
                    status = "Active"
                )
            )
            subscriptionDao.insertPlan(
                SubscriptionPlan(
                    id = "enterprise_id",
                    name = "Enterprise Plan",
                    price = 499.0,
                    durationDays = 365,
                    maxProjects = 999,
                    maxOwners = 2,
                    maxContractors = 5,
                    maxEngineers = 2,
                    storageLimitGb = 1000,
                    status = "Active"
                )
            )

            // Add a Default Super Admin
            userDao.insertUser(
                User(
                    id = "admin_user_id",
                    firstName = "Super",
                    lastName = "Admin",
                    email = "admin@cpdms.com",
                    phoneNumber = "+123456789",
                    passwordHash = "admin123", // Simplified for MVP
                    role = "Super Admin",
                    accountStatus = "Active",
                    emailVerified = true,
                    createdDate = System.currentTimeMillis()
                )
            )

            // Add a Default Engineering Office
            val eoCompanyId = "eo_company_id"
            companyDao.insertCompany(
                Company(
                    id = eoCompanyId,
                    name = "Apex Engineering Solutions",
                    logo = "ic_apex",
                    country = "Saudi Arabia",
                    city = "Riyadh",
                    address = "Olaya District, King Fahd Rd",
                    phone = "+966112223344",
                    email = "contact@apexeng.com",
                    registrationNumber = "CR-1010303020",
                    taxNumber = "TAX-300020001000003",
                    status = "Active",
                    subscriptionPlanId = "pro_id"
                )
            )

            subscriptionDao.insertSubscription(
                Subscription(
                    id = "sub_apex_id",
                    companyId = eoCompanyId,
                    planId = "pro_id",
                    startDate = System.currentTimeMillis(),
                    endDate = System.currentTimeMillis() + 30L * 24L * 60L * 60L * 1000L,
                    status = "Active",
                    paymentStatus = "Paid"
                )
            )

            val eoUserId = "eo_user_id"
            userDao.insertUser(
                User(
                    id = eoUserId,
                    firstName = "Ahmad",
                    lastName = "Al-Saeed",
                    email = "eo@cpdms.com",
                    phoneNumber = "+966501234567",
                    passwordHash = "eo123",
                    role = "Engineering Office",
                    accountStatus = "Active",
                    emailVerified = true,
                    createdDate = System.currentTimeMillis(),
                    companyId = eoCompanyId
                )
            )

            // Create a default project
            val projId = "proj_apex_1"
            projectDao.insertProject(
                Project(
                    id = projId,
                    companyId = eoCompanyId,
                    name = "Al-Yasmin Residential Complex",
                    description = "Construction of a premium modern 4-villa residential complex in Riyadh.",
                    location = "Al-Yasmin, Riyadh",
                    projectType = "Residential Building",
                    startDate = System.currentTimeMillis() - 15L * 24L * 60L * 60L * 1000L,
                    expectedEndDate = System.currentTimeMillis() + 180L * 24L * 60L * 60L * 1000L,
                    status = "Active",
                    createdBy = eoUserId
                )
            )

            // Add EO to the project members
            projectDao.insertMember(
                ProjectMember(
                    id = "mem_eo_1",
                    projectId = projId,
                    userId = eoUserId,
                    role = "Engineering Office",
                    joinDate = System.currentTimeMillis(),
                    permissionSet = "ALL"
                )
            )

            // Let's pre-generate invitation codes for this project
            invitationCodeDao.insertCode(
                InvitationCode(
                    id = "code_owner",
                    code = "OWN-777",
                    projectId = projId,
                    userType = "Owner",
                    createdBy = eoUserId,
                    expiryDate = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                    status = "Active"
                )
            )

            invitationCodeDao.insertCode(
                InvitationCode(
                    id = "code_eng",
                    code = "ENG-888",
                    projectId = projId,
                    userType = "Resident Engineer",
                    createdBy = eoUserId,
                    expiryDate = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                    status = "Active"
                )
            )

            invitationCodeDao.insertCode(
                InvitationCode(
                    id = "code_con_struct",
                    code = "CON-STR",
                    projectId = projId,
                    userType = "Contractor",
                    contractorType = "Structural Contractor",
                    createdBy = eoUserId,
                    expiryDate = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                    status = "Active"
                )
            )

            invitationCodeDao.insertCode(
                InvitationCode(
                    id = "code_con_finish",
                    code = "CON-FIN",
                    projectId = projId,
                    userType = "Contractor",
                    contractorType = "Finishing Contractor",
                    createdBy = eoUserId,
                    expiryDate = System.currentTimeMillis() + 7L * 24L * 60L * 60L * 1000L,
                    status = "Active"
                )
            )

            // Let's also pre-populate some demo roles who have already joined the project to make dashboards rich immediately
            val ownerId = "owner_user_id"
            userDao.insertUser(
                User(
                    id = ownerId,
                    firstName = "Khalid",
                    lastName = "Al-Faisal",
                    email = "owner@cpdms.com",
                    phoneNumber = "+966507777777",
                    passwordHash = "owner123",
                    role = "Owner",
                    accountStatus = "Active",
                    emailVerified = true,
                    createdDate = System.currentTimeMillis()
                )
            )
            projectDao.insertMember(
                ProjectMember(
                    id = "mem_owner_1",
                    projectId = projId,
                    userId = ownerId,
                    role = "Owner",
                    joinDate = System.currentTimeMillis(),
                    permissionSet = "VIEW"
                )
            )

            val engId = "engineer_user_id"
            userDao.insertUser(
                User(
                    id = engId,
                    firstName = "Sami",
                    lastName = "Hassan",
                    email = "engineer@cpdms.com",
                    phoneNumber = "+966508888888",
                    passwordHash = "engineer123",
                    role = "Resident Engineer",
                    accountStatus = "Active",
                    emailVerified = true,
                    createdDate = System.currentTimeMillis()
                )
            )
            projectDao.insertMember(
                ProjectMember(
                    id = "mem_eng_1",
                    projectId = projId,
                    userId = engId,
                    role = "Resident Engineer",
                    joinDate = System.currentTimeMillis(),
                    permissionSet = "REVIEW"
                )
            )

            val contractorId = "contractor_user_id"
            userDao.insertUser(
                User(
                    id = contractorId,
                    firstName = "Youssef",
                    lastName = "Builds",
                    email = "contractor@cpdms.com",
                    phoneNumber = "+966509999999",
                    passwordHash = "contractor123",
                    role = "Contractor",
                    accountStatus = "Active",
                    emailVerified = true,
                    createdDate = System.currentTimeMillis()
                )
            )
            projectDao.insertMember(
                ProjectMember(
                    id = "mem_contractor_1",
                    projectId = projId,
                    userId = contractorId,
                    role = "Contractor",
                    contractorType = "Structural Contractor",
                    joinDate = System.currentTimeMillis(),
                    permissionSet = "SUBMIT"
                )
            )

            // Let's create stages
            val stage1Id = "stage_1"
            val stage2Id = "stage_2"
            stageDao.insertStage(
                Stage(
                    id = stage1Id,
                    projectId = projId,
                    name = "Excavation and Site Preparation",
                    description = "Site leveling, perimeter fencing, soil testing, and primary excavation works.",
                    startDate = System.currentTimeMillis() - 15L * 24L * 60L * 60L * 1000L,
                    endDate = System.currentTimeMillis() - 5L * 24L * 60L * 60L * 1000L,
                    status = "Completed",
                    assignedContractorId = contractorId,
                    assignedEngineerId = engId
                )
            )

            stageDao.insertStage(
                Stage(
                    id = stage2Id,
                    projectId = projId,
                    name = "Foundation Work",
                    description = "Pouring blind concrete, layout setting, rebar installation, and concrete casting.",
                    startDate = System.currentTimeMillis() - 4L * 24L * 60L * 60L * 1000L,
                    endDate = System.currentTimeMillis() + 20L * 24L * 60L * 60L * 1000L,
                    status = "In Progress",
                    assignedContractorId = contractorId,
                    assignedEngineerId = engId
                )
            )

            // Create work items for Foundation Stage
            val item1Id = "item_1"
            val item2Id = "item_2"
            workItemDao.insertItem(
                WorkItem(
                    id = item1Id,
                    stageId = stage2Id,
                    contractorId = contractorId,
                    engineerId = engId,
                    name = "Rebar Reinforcement Inspection",
                    description = "Verify reinforcing steel spacing, size, and tying according to construction blueprints.",
                    dueDate = System.currentTimeMillis() + 2L * 24L * 60L * 60L * 1000L,
                    status = "Submitted For Review",
                    completionPercentage = 100
                )
            )

            workItemDao.insertItem(
                WorkItem(
                    id = item2Id,
                    stageId = stage2Id,
                    contractorId = contractorId,
                    engineerId = engId,
                    name = "Formwork and Shuttering Alignment",
                    description = "Formwork must be plumb and sturdy to sustain heavy fresh concrete weight.",
                    dueDate = System.currentTimeMillis() + 4L * 24L * 60L * 60L * 1000L,
                    status = "In Progress",
                    completionPercentage = 60
                )
            )

            // Pre-add comments
            commentDao.insertComment(
                Comment(
                    id = "comment_1",
                    userId = contractorId,
                    userName = "Youssef (Contractor)",
                    userRole = "Contractor",
                    projectId = projId,
                    itemId = item1Id,
                    parentId = null,
                    commentText = "All rebars have been bound and checked. Ready for official site survey and casting approval.",
                    createdDate = System.currentTimeMillis() - 4L * 3600 * 1000
                )
            )

            commentDao.insertComment(
                Comment(
                    id = "comment_2",
                    userId = engId,
                    userName = "Sami (Resident Engineer)",
                    userRole = "Resident Engineer",
                    projectId = projId,
                    itemId = item1Id,
                    parentId = "comment_1",
                    commentText = "Please double check the tie-wire ties on the column starter links. Ensure we have 5cm clear cover on all sides.",
                    createdDate = System.currentTimeMillis() - 2L * 3600 * 1000
                )
            )

            // Add notifications for users
            notificationDao.insertNotification(
                Notification(
                    id = UUID.randomUUID().toString(),
                    userId = engId,
                    title = "Work Submitted",
                    message = "Contractor submitted 'Rebar Reinforcement Inspection' for review on Foundation Work stage.",
                    type = "approval",
                    readStatus = false,
                    createdDate = System.currentTimeMillis() - 2L * 3600 * 1000
                )
            )

            notificationDao.insertNotification(
                Notification(
                    id = UUID.randomUUID().toString(),
                    userId = ownerId,
                    title = "Project Progress Update",
                    message = "Al-Yasmin Residential Complex project progress reaches 35%.",
                    type = "communication",
                    readStatus = false,
                    createdDate = System.currentTimeMillis() - 5L * 3600 * 1000
                )
            )
        }
    }
}
