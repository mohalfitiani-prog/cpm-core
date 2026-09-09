enum ProjectRole { office, residentEngineer, contractor, owner }

enum ProjectAction {
  manageProject,
  manageOffice,
  manageTeam,
  createStage,
  createItem,
  submitDelivery,
  reviewDelivery,
  createRfi,
  submitNcr,
  reviewNcr,
  writeNcrProcedure,
  uploadAttachment,
  publishAttachment,
  submitMinutes,
  publishMinutes,
  viewTeam,
}

/// UI policy only. The backend must independently enforce the same rules.
/// Resolve roles from membership in this project, never a global user role.
class ProjectPolicy {
  static const maxStages = 10;
  static const maxItemsPerStage = 5;
  static const maxOwners = 1;
  static const maxContractors = 3;
  static const maxResidentEngineers = 1;

  static bool allows(ProjectRole role, ProjectAction action) {
    if (role == ProjectRole.office) return true;
    switch (action) {
      case ProjectAction.manageProject:
      case ProjectAction.manageOffice:
      case ProjectAction.manageTeam:
        return false;
      case ProjectAction.createStage:
      case ProjectAction.reviewDelivery:
      case ProjectAction.reviewNcr:
      case ProjectAction.writeNcrProcedure:
      case ProjectAction.publishAttachment:
      case ProjectAction.publishMinutes:
        return role == ProjectRole.residentEngineer;
      case ProjectAction.createItem:
      case ProjectAction.submitDelivery:
        return role != ProjectRole.owner;
      case ProjectAction.createRfi:
      case ProjectAction.submitNcr:
      case ProjectAction.uploadAttachment:
      case ProjectAction.submitMinutes:
      case ProjectAction.viewTeam:
        return true;
    }
  }

  static bool canReplyToRfi({
    required String actorId,
    required ProjectRole actorRole,
    String? recipientId,
    ProjectRole? recipientRole,
  }) {
    // A person-specific recipient takes precedence over a role recipient.
    if (recipientId != null) return actorId == recipientId;
    return recipientRole != null && actorRole == recipientRole;
  }

  static double progress({required int approved, required int total}) {
    if (approved < 0 || total < 0 || approved > total) {
      throw ArgumentError('Invalid approved/total item counts');
    }
    return total == 0 ? 0 : approved / total;
  }
}

enum ItemStatus {
  notStarted,
  inProgress,
  awaitingApproval,
  approved,
  needsRevision,
}

enum NcrStatus { awaitingApproval, approved, rejected, closed }

/// No automatic deletion: policy after the retention year is undecided.
enum RetentionDecision { retainActive, retainGracePeriod, manualPolicyReview }

RetentionDecision retentionDecision({
  required bool subscriptionActive,
  required DateTime now,
  DateTime? stoppedAt,
}) {
  if (subscriptionActive) return RetentionDecision.retainActive;
  if (stoppedAt == null) return RetentionDecision.manualPolicyReview;
  final stopped = stoppedAt.toUtc();
  final nextYear = stopped.year + 1;
  final lastDay = DateTime.utc(nextYear, stopped.month + 1, 0).day;
  final anniversary = DateTime.utc(
    nextYear,
    stopped.month,
    stopped.day > lastDay ? lastDay : stopped.day,
    stopped.hour,
    stopped.minute,
    stopped.second,
    stopped.millisecond,
    stopped.microsecond,
  );
  return now.toUtc().isBefore(anniversary)
      ? RetentionDecision.retainGracePeriod
      : RetentionDecision.manualPolicyReview;
}
