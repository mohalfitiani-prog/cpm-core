import 'package:flutter/material.dart';

import 'api.dart';
import 'main.dart' show form, error;

const roles = {
  'office': 'المكتب الهندسي',
  'residentEngineer': 'المهندس المقيم',
  'contractor': 'المقاول',
  'owner': 'المالك',
};
const statuses = {
  'notStarted': 'لم يبدأ',
  'inProgress': 'قيد التنفيذ',
  'awaitingApproval': 'بانتظار الاعتماد',
  'approved': 'معتمد',
  'needsRevision': 'يحتاج تعديل',
  'pending': 'بانتظار الموافقة',
  'rejected': 'مرفوض',
  'closed': 'مغلق',
};

class ProjectPage extends StatefulWidget {
  final String id;
  const ProjectPage({super.key, required this.id});
  @override
  State<ProjectPage> createState() => _ProjectPageState();
}

class _ProjectPageState extends State<ProjectPage> {
  late Future<Json> future;
  Json? project;
  int tab = 0;
  bool busy = false;
  String get uid => Api.auth.currentUser!.uid;
  List get members => project!['members'];
  String get role => members.firstWhere((m) => m['uid'] == uid)['role'];
  bool get eng => ['office', 'residentEngineer'].contains(role);
  bool get office => role == 'office';
  @override
  void initState() {
    super.initState();
    refresh();
  }

  void refresh() {
    setState(() {
      future = Api.call('getProject', {'projectId': widget.id});
    });
  }

  Future<void> run(Future<void> Function() f) async {
    if (busy) return;
    setState(() => busy = true);
    try {
      await f();
      if (mounted) refresh();
    } catch (e) {
      if (mounted) error(context, e);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  Future<void> action(String name, Json data) => run(() async {
    await Api.call('projectAction', {
      'projectId': widget.id,
      'action': name,
      'payload': data,
    });
  });
  Future<String?> image() async =>
      showModalBottomSheet<String>(
        context: context,
        builder: (c) => SafeArea(
          child: Wrap(
            children: [
              ListTile(
                leading: const Icon(Icons.camera_alt),
                title: const Text('التقاط صورة'),
                onTap: () => Navigator.pop(c, 'camera'),
              ),
              ListTile(
                leading: const Icon(Icons.photo),
                title: const Text('اختيار صورة'),
                onTap: () => Navigator.pop(c, 'gallery'),
              ),
            ],
          ),
        ),
      ).then(
        (v) async => v == null
            ? null
            : Api.upload(widget.id, camera: v == 'camera', image: true, context: context),
      );
  String person(String id) => members.firstWhere(
    (m) => m['uid'] == id,
    orElse: () => {'name': 'عضو سابق'},
  )['name'];
  String memberLabel(dynamic m) =>
      '${m['name']} — ${roles[m['role']]} (${m['uid'].toString().substring(0, 6)})';
  Future<void> addRfi({String? stageId, String? itemId}) async {
    final choices = members.map((m) => memberLabel(m)).toList();
    final d = await form(
      context,
      'طلب استفسار RFI',
      {
        'subject': 'الموضوع',
        'description': 'الوصف',
        'recipient': 'المستلم',
        'importance': 'الأهمية',
      },
      choices: {
        'recipient': choices,
        'importance': ['عادي', 'مهم جداً'],
      },
    );
    if (d == null) return;
    await run(() async {
      final photo = await image();
      await Api.call('projectAction', {
        'projectId': widget.id,
        'action': 'rfi',
        'payload': {
          ...d,
          'recipient': members.firstWhere(
            (m) => memberLabel(m) == d['recipient'],
          )['uid'],
          'importance': d['importance'] == 'مهم جداً' ? 'urgent' : 'normal',
          'stageId': stageId,
          'itemId': itemId,
          'photo': photo,
        },
      });
    });
  }

  Widget button(String label, IconData icon, VoidCallback fn) => Padding(
    padding: const EdgeInsets.only(left: 8, bottom: 8),
    child: OutlinedButton.icon(
      onPressed: busy ? null : fn,
      icon: Icon(icon, size: 18),
      label: Text(label),
    ),
  );
  Widget card(String title, List<Widget> children, {String? subtitle}) => Card(
    margin: const EdgeInsets.only(bottom: 16),
    child: Padding(
      padding: const EdgeInsets.all(18),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(title, style: Theme.of(context).textTheme.titleMedium),
          if (subtitle != null)
            Padding(
              padding: const EdgeInsets.symmetric(vertical: 8),
              child: Text(subtitle),
            ),
          ...children,
        ],
      ),
    ),
  );
  Widget overview() {
    final items = [for (final s in project!['stages']) ...s['items'] as List];
    final approved = items.where((i) => i['status'] == 'approved').length;
    final urgent = (project!['rfi'] as List).where(
      (r) => r['importance'] == 'urgent' && r['reply'] == null,
    );
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        card('تقدم المشروع', [
          const SizedBox(height: 16),
          LinearProgressIndicator(
            value: items.isEmpty ? 0 : approved / items.length,
            minHeight: 10,
            borderRadius: BorderRadius.circular(8),
          ),
          const SizedBox(height: 12),
          Text('$approved من ${items.length} بند معتمد'),
        ]),
        card('معلومات المشروع', [
          Text('النوع: ${project!['type']}'),
          Text('الموقع: ${project!['location']}'),
          Text('الحالة: ${project!['status']}'),
          Text(project!['description']),
        ]),
        Wrap(
          children: [
            Chip(
              label: Text(
                '${(project!['rfi'] as List).where((r) => r['reply'] == null).length} استفسارات دون رد',
              ),
            ),
            Chip(
              label: Text(
                '${items.where((i) => i['status'] == 'awaitingApproval').length} تسليمات بانتظار الاعتماد',
              ),
            ),
          ],
        ),
        for (final r in urgent)
          card('مهم جداً: ${r['subject']}', [Text(r['description'])]),
      ],
    );
  }

  Widget stages() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      if (eng)
        button(
          'إنشاء مرحلة (${project!['stages'].length}/10)',
          Icons.add,
          () async {
            final labels = members.map((m) => memberLabel(m)).toList();
            final d = await form(
              context,
              'مرحلة جديدة',
              {
                'name': 'اسم المرحلة',
                'description': 'الوصف',
                'responsible': 'مسؤول التسليم',
                'due': 'التاريخ المتوقع YYYY-MM-DD',
                'details': 'تفاصيل',
              },
              choices: {'responsible': labels},
            );
            if (d != null)
              await action('stage', {
                ...d,
                'responsible': members.firstWhere(
                  (m) => memberLabel(m) == d['responsible'],
                )['uid'],
              });
          },
        ),
      for (final s in project!['stages'])
        Card(
          child: ExpansionTile(
            title: Text(s['name']),
            subtitle: Text(
              '${s['description']}\nالتسليم: ${s['due']} — ${person(s['responsible'])}',
            ),
            childrenPadding: const EdgeInsets.all(12),
            children: [
              Wrap(
                children: [
                  button(
                    'طلب RFI',
                    Icons.help_outline,
                    () => addRfi(stageId: s['id']),
                  ),
                  if (role != 'owner')
                    button(
                      'إضافة بند (${s['items'].length}/5)',
                      Icons.add,
                      () async {
                        final d = await form(context, 'بند جديد', {
                          'name': 'اسم البند',
                          'description': 'الوصف',
                          'conditions': 'شروط التسليم',
                        });
                        if (d != null)
                          await action('item', {...d, 'stageId': s['id']});
                      },
                    ),
                ],
              ),
              for (final i in s['items'])
                card(i['name'], [
                  Text(i['description']),
                  Text('شروط التسليم: ${i['conditions']}'),
                  Chip(label: Text(statuses[i['status']] ?? i['status'])),
                  for (final delivery in i['deliveries'])
                    ListTile(
                      title: Text(delivery['description']),
                      subtitle: Text(person(delivery['author'])),
                      trailing: IconButton(
                        onPressed: () => run(() => Api.open(delivery['photo'])),
                        icon: const Icon(Icons.image),
                      ),
                    ),
                  if (i['review'] != null)
                    Text('ملاحظة المراجعة: ${i['review']['note']}'),
                  Wrap(
                    children: [
                      button(
                        'طلب RFI',
                        Icons.help_outline,
                        () => addRfi(stageId: s['id'], itemId: i['id']),
                      ),
                      if (role != 'owner' && i['status'] == 'notStarted')
                        button(
                          'بدء العمل',
                          Icons.play_arrow,
                          () => action('start', {
                            'stageId': s['id'],
                            'itemId': i['id'],
                          }),
                        ),
                      if (role != 'owner' &&
                          [
                            'notStarted',
                            'inProgress',
                            'needsRevision',
                          ].contains(i['status']))
                        button('تسليم بالصور', Icons.camera_alt, () async {
                          final d = await form(context, 'وصف التسليم', {
                            'description': 'وصف الصورة والتسليم',
                          });
                          if (d == null) return;
                          await run(() async {
                            final photo = await image();
                            if (photo == null) return;
                            await Api.call('projectAction', {
                              'projectId': widget.id,
                              'action': 'deliver',
                              'payload': {
                                ...d,
                                'photo': photo,
                                'stageId': s['id'],
                                'itemId': i['id'],
                              },
                            });
                          });
                        }),
                      if (eng && i['status'] == 'awaitingApproval')
                        ...['approved', 'needsRevision'].map(
                          (status) => button(
                            status == 'approved' ? 'اعتماد' : 'رفض / طلب تعديل',
                            status == 'approved' ? Icons.check : Icons.edit,
                            () async {
                              final d = await form(context, 'مراجعة التسليم', {
                                'note': 'ملاحظات المكتب',
                              });
                              if (d != null)
                                await action('reviewDelivery', {
                                  ...d,
                                  'status': status,
                                  'stageId': s['id'],
                                  'itemId': i['id'],
                                });
                            },
                          ),
                        ),
                    ],
                  ),
                ]),
            ],
          ),
        ),
    ],
  );
  Widget rfi() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      button('طلب استفسار', Icons.add, () => addRfi()),
      for (final r in project!['rfi'])
        card(r['subject'], [
          Text(r['description']),
          Text('من ${person(r['author'])} إلى ${person(r['recipient'])}'),
          if (r['stageId'] != null) const Text('مرتبط بمرحلة أو بند'),
          if (r['importance'] == 'urgent') const Chip(label: Text('مهم جداً')),
          if (r['photo'] != null)
            button(
              'الصورة',
              Icons.image,
              () => run(() => Api.open(r['photo'])),
            ),
          if (r['reply'] != null)
            Text('الرد: ${r['reply']['text']}')
          else if (r['recipient'] == uid)
            button('إجابة', Icons.reply, () async {
              final d = await form(context, 'الإجابة على الاستفسار', {
                'reply': 'الإجابة',
              });
              if (d != null) await action('replyRfi', {...d, 'id': r['id']});
            })
          else
            const Text('بانتظار الرد'),
        ]),
    ],
  );
  Widget ncr() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      button('تقديم NCR', Icons.add, () async {
        final d = await form(context, 'تقرير عدم مطابقة', {
          'description': 'وصف المشكلة',
          'violation': 'نوع المخالفة',
        });
        if (d == null) return;
        await run(() async {
          final photo = await image();
          if (photo == null) return;
          await Api.call('projectAction', {
            'projectId': widget.id,
            'action': 'ncr',
            'payload': {...d, 'photo': photo},
          });
        });
      }),
      for (final n in project!['ncr'])
        card(n['violation'], [
          Text(n['description']),
          Chip(label: Text(statuses[n['status']] ?? n['status'])),
          button(
            'صورة المخالفة',
            Icons.image,
            () => run(() => Api.open(n['photo'])),
          ),
          if (n['procedure'] != '')
            Text('الإجراءات المتبعة: ${n['procedure']}'),
          if (eng && ['pending', 'approved'].contains(n['status']))
            Wrap(
              children: [
                for (final status
                    in n['status'] == 'pending'
                        ? ['approved', 'rejected']
                        : ['closed'])
                  button(statuses[status]!, Icons.fact_check, () async {
                    final d = await form(context, 'مراجعة NCR', {
                      'procedure': 'الإجراءات المتبعة',
                    });
                    if (d != null)
                      await action('reviewNcr', {
                        ...d,
                        'id': n['id'],
                        'status': status,
                      });
                  }),
              ],
            ),
        ]),
    ],
  );
  Widget files() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      button('إضافة مرفق', Icons.attach_file, () async {
        final d = await form(
          context,
          'مرفق جديد',
          {'name': 'اسم المرفق', 'category': 'التصنيف'},
          choices: {
            'category': ['مخططات', 'عقود', 'مرفقات أخرى'],
          },
        );
        if (d == null) return;
        await run(() async {
          final path = await Api.upload(widget.id);
          if (path == null) return;
          await Api.call('projectAction', {
            'projectId': widget.id,
            'action': 'file',
            'payload': {...d, 'path': path},
          });
        });
      }),
      for (final f in project!['files'])
        card(f['name'], [
          Text('${f['category']} — ${statuses[f['status']]}'),
          button(
            'فتح الملف',
            Icons.open_in_new,
            () => run(() => Api.open(f['path'])),
          ),
          moderation('files', f),
        ]),
    ],
  );
  Widget moderation(String collection, dynamic r) =>
      eng && r['status'] == 'pending'
      ? Wrap(
          children: [
            for (final s in ['approved', 'rejected'])
              button(
                statuses[s]!,
                s == 'approved' ? Icons.check : Icons.close,
                () => action('moderate', {
                  'id': r['id'],
                  'collection': collection,
                  'status': s,
                }),
              ),
          ],
        )
      : const SizedBox.shrink();
  Widget minutes() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      button('إضافة محضر اجتماع', Icons.add, () async {
        final d = await form(context, 'محضر اجتماع', {
          'title': 'عنوان الاجتماع',
          'date': 'التاريخ YYYY-MM-DD',
          'attendees': 'الحضور',
          'decisions': 'القرارات والمعلومات المتفق عليها',
        });
        if(d==null)return;
        await run(() async {
          final attach=await showDialog<bool>(context:context,builder:(c)=>AlertDialog(title:const Text('إضافة مرفق للمحضر؟'),actions:[TextButton(onPressed:()=>Navigator.pop(c,false),child:const Text('بدون مرفق')),FilledButton(onPressed:()=>Navigator.pop(c,true),child:const Text('اختيار ملف'))]));
          if(attach==null)return;
          final path=attach?await Api.upload(widget.id):null;
          if(attach&&path==null)return;
          await Api.call('projectAction',{'projectId':widget.id,'action':'minutes','payload':{...d,'attachment':path}});
        });
      }),
      for (final r in project!['minutes'])
        card(r['title'], [
          Text('التاريخ: ${r['date']}'),
          Text('الحضور: ${r['attendees']}'),
          Text(r['decisions']),
          if(r['attachment']!=null)button('مرفق المحضر',Icons.attach_file,()=>run(()=>Api.open(r['attachment']))),
          Chip(label: Text(statuses[r['status']] ?? r['status'])),
          moderation('minutes', r),
        ]),
    ],
  );
  Widget team() => Column(
    crossAxisAlignment: CrossAxisAlignment.stretch,
    children: [
      if (office)
        button('توليد كود إضافة', Icons.key, () async {
          final d = await form(
            context,
            'إضافة عضو للفريق',
            {'role': 'الدور'},
            choices: {
              'role': ['المالك', 'المقاول', 'المهندس المقيم'],
            },
          );
          if (d == null) return;
          await run(() async {
            final r = await Api.call('invite', {
              'projectId': widget.id,
              'role': roles.entries.firstWhere((e) => e.value == d['role']).key,
            });
            if (mounted)
              await showDialog(
                context: context,
                builder: (c) => AlertDialog(
                  title: const Text('كود الانضمام — صالح 7 أيام'),
                  content: SelectableText(
                    r['code'],
                    textDirection: TextDirection.ltr,
                  ),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(c),
                      child: const Text('تم'),
                    ),
                  ],
                ),
              );
          });
        }),
      for (final m in members)
        card(m['name'], [
          Text(roles[m['role']] ?? m['role']),
          Text('الهاتف: ${m['phone']}'),
          Text('العنوان: ${m['address'] ?? ''}'),
          Text('بدء الخدمة: ${m['start'] ?? ''}'),
          Text('انتهاء الخدمة: ${m['end'] ?? ''}'),
          if (office && m['uid'] != uid)
            button('إزالة العضو', Icons.person_remove, () async {
              final yes = await showDialog<bool>(
                context: context,
                builder: (c) => AlertDialog(
                  title: Text('إزالة ${m['name']} من المشروع؟'),
                  actions: [
                    TextButton(
                      onPressed: () => Navigator.pop(c, false),
                      child: const Text('إلغاء'),
                    ),
                    FilledButton(
                      onPressed: () => Navigator.pop(c, true),
                      child: const Text('إزالة'),
                    ),
                  ],
                ),
              );
              if (yes == true) await action('removeMember', {'uid': m['uid']});
            }),
        ]),
    ],
  );
  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: Text(project?['name'] ?? 'المشروع'),
      actions: [
        IconButton(
          onPressed: busy ? null : refresh,
          icon: const Icon(Icons.refresh),
        ),
      ],
    ),
    body: FutureBuilder<Json>(
      future: future,
      builder: (c, s) {
        if (s.hasError)
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text('${s.error}', textAlign: TextAlign.center),
                TextButton(
                  onPressed: refresh,
                  child: const Text('إعادة المحاولة'),
                ),
              ],
            ),
          );
        if (!s.hasData) return const Center(child: CircularProgressIndicator());
        project = s.data!;
        final views = [overview, stages, rfi, ncr, files, minutes, team];
        return Column(
          children: [
            if (busy) const LinearProgressIndicator(),
            Expanded(
              child: AbsorbPointer(
                absorbing: busy,
                child: ListView(
                  padding: const EdgeInsets.all(18),
                  children: [views[tab]()],
                ),
              ),
            ),
          ],
        );
      },
    ),
    bottomNavigationBar: SafeArea(
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            for (final e in [
              'نظرة عامة',
              'المراحل والاعتماد',
              'RFI',
              'NCR',
              'المخططات',
              'اجتماعات ومحاضر',
              'فريق العمل',
            ].asMap().entries)
              Padding(
                padding: const EdgeInsets.all(4),
                child: ChoiceChip(
                  label: Text(e.value),
                  selected: tab == e.key,
                  onSelected: busy ? null : (v) => setState(() => tab = e.key),
                ),
              ),
          ],
        ),
      ),
    ),
  );
}
