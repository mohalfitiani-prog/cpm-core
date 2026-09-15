import 'package:flutter_test/flutter_test.dart';
import 'package:cpm_core/domain/project_policy.dart';

void main() {
  test('resident engineer reviews work but cannot administer office', () {
    expect(
      ProjectPolicy.allows(
        ProjectRole.residentEngineer,
        ProjectAction.reviewDelivery,
      ),
      isTrue,
    );
    for (final action in [
      ProjectAction.manageOffice,
      ProjectAction.manageProject,
      ProjectAction.manageTeam,
    ]) {
      expect(
        ProjectPolicy.allows(ProjectRole.residentEngineer, action),
        isFalse,
      );
    }
  });
  test('owner can request information but cannot submit construction work', () {
    expect(
      ProjectPolicy.allows(ProjectRole.owner, ProjectAction.createRfi),
      isTrue,
    );
    expect(
      ProjectPolicy.allows(ProjectRole.owner, ProjectAction.createItem),
      isFalse,
    );
    expect(
      ProjectPolicy.allows(ProjectRole.owner, ProjectAction.submitDelivery),
      isFalse,
    );
  });
  test('contractor submissions require engineering approval', () {
    expect(
      ProjectPolicy.allows(ProjectRole.contractor, ProjectAction.submitNcr),
      isTrue,
    );
    expect(
      ProjectPolicy.allows(ProjectRole.contractor, ProjectAction.reviewNcr),
      isFalse,
    );
    expect(
      ProjectPolicy.allows(
        ProjectRole.contractor,
        ProjectAction.writeNcrProcedure,
      ),
      isFalse,
    );
    expect(
      ProjectPolicy.allows(
        ProjectRole.contractor,
        ProjectAction.publishAttachment,
      ),
      isFalse,
    );
  });
  test(
    'a targeted RFI cannot be answered by another person in the same role',
    () {
      expect(
        ProjectPolicy.canReplyToRfi(
          actorId: 'b',
          actorRole: ProjectRole.contractor,
          recipientId: 'a',
          recipientRole: ProjectRole.contractor,
        ),
        isFalse,
      );
    },
  );
  test('progress counts approved items, including empty projects', () {
    expect(ProjectPolicy.progress(approved: 2, total: 5), 0.4);
    expect(ProjectPolicy.progress(approved: 0, total: 0), 0);
    expect(
      () => ProjectPolicy.progress(approved: 3, total: 2),
      throwsArgumentError,
    );
  });
  test('retention never automatically authorizes deletion', () {
    expect(
      retentionDecision(
        subscriptionActive: false,
        stoppedAt: DateTime.utc(2026, 9, 9),
        now: DateTime.utc(2027, 9, 8),
      ),
      RetentionDecision.retainGracePeriod,
    );
    expect(
      retentionDecision(
        subscriptionActive: false,
        stoppedAt: DateTime.utc(2026, 9, 9),
        now: DateTime.utc(2027, 9, 9),
      ),
      RetentionDecision.manualPolicyReview,
    );
  });
}
