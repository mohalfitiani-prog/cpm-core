import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';

import 'api.dart';
import 'firebase_options.dart';
import 'project.dart';

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  try {
    await Firebase.initializeApp(options: androidFirebaseOptions);
    runApp(const CpmApp());
  } catch (e) {
    runApp(
      MaterialApp(
        home: Scaffold(
          body: Center(
            child: Text(
              'تعذر تهيئة الاتصال. تأكد من إعداد Firebase ثم أعد تشغيل التطبيق.\n$e',
            ),
          ),
        ),
      ),
    );
  }
}

class CpmApp extends StatelessWidget {
  const CpmApp({super.key});
  @override
  Widget build(BuildContext context) => MaterialApp(
    debugShowCheckedModeBanner: false,
    title: 'CPM Core',
    locale: const Locale('ar'),
    supportedLocales: const [Locale('ar')],
    localizationsDelegates: GlobalMaterialLocalizations.delegates,
    theme: ThemeData(
      useMaterial3: true,
      colorScheme: ColorScheme.fromSeed(seedColor: const Color(0xff143d59)),
      scaffoldBackgroundColor: const Color(0xfff3f6fa),
      inputDecorationTheme: const InputDecorationTheme(
        border: OutlineInputBorder(),
      ),
      appBarTheme: const AppBarTheme(centerTitle: false),
    ),
    home: StreamBuilder<User?>(
      stream: Api.auth.authStateChanges(),
      builder: (c, s) => s.connectionState == ConnectionState.waiting
          ? const Scaffold(body: Center(child: CircularProgressIndicator()))
          : s.data == null
          ? const LoginPage()
          : const Dashboard(),
    ),
  );
}

void error(BuildContext c, Object e) {
  ScaffoldMessenger.of(c).showSnackBar(
    SnackBar(
      content: Text(
        e is FirebaseException ? e.message ?? e.code : e.toString(),
      ),
    ),
  );
}

Future<Map<String, String>?> form(
  BuildContext c,
  String title,
  Map<String, String> fields, {
  Map<String, List<String>> choices = const {},
}) async {
  final controllers = {for (final k in fields.keys) k: TextEditingController()};
  final key = GlobalKey<FormState>();
  final result = await showDialog<Map<String, String>>(
    context: c,
    builder: (ctx) => AlertDialog(
      title: Text(title),
      content: SizedBox(
        width: 480,
        child: SingleChildScrollView(
          child: Form(
            key: key,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: fields.entries
                  .map(
                    (f) => Padding(
                      padding: const EdgeInsets.only(bottom: 14),
                      child: choices.containsKey(f.key)
                          ? DropdownButtonFormField<String>(
                              decoration: InputDecoration(labelText: f.value),
                              items: choices[f.key]!
                                  .map(
                                    (s) => DropdownMenuItem(
                                      value: s,
                                      child: Text(s),
                                    ),
                                  )
                                  .toList(),
                              onChanged: (v) =>
                                  controllers[f.key]!.text = v ?? '',
                              validator: (v) => v == null ? 'مطلوب' : null,
                            )
                          : TextFormField(
                              controller: controllers[f.key],
                              decoration: InputDecoration(labelText: f.value),
                              maxLines:
                                  f.key == 'description' || f.key == 'decisions'
                                  ? 3
                                  : 1,
                              validator: (v) =>
                                  (v ?? '').trim().isEmpty ? 'مطلوب' : null,
                            ),
                    ),
                  )
                  .toList(),
            ),
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(ctx),
          child: const Text('إلغاء'),
        ),
        FilledButton(
          onPressed: () {
            if (key.currentState!.validate())
              Navigator.pop(ctx, {
                for (final e in controllers.entries) e.key: e.value.text.trim(),
              });
          },
          child: const Text('حفظ'),
        ),
      ],
    ),
  );
  // Dispose after the dialog route has finished its closing animation.
  Future.delayed(const Duration(milliseconds: 400), () {
    for (final v in controllers.values) v.dispose();
  });
  return result;
}

class LoginPage extends StatefulWidget {
  const LoginPage({super.key});
  @override
  State<LoginPage> createState() => _LoginPageState();
}

class _LoginPageState extends State<LoginPage> {
  final login = TextEditingController(), password = TextEditingController();
  bool busy = false, obscure = true;
  @override
  void dispose() {
    login.dispose();
    password.dispose();
    super.dispose();
  }

  Future<void> run(Future<void> Function() fn) async {
    setState(() => busy = true);
    try {
      await fn();
    } catch (e) {
      if (mounted) error(context, e);
    } finally {
      if (mounted) setState(() => busy = false);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    body: Center(
      child: SingleChildScrollView(
        padding: const EdgeInsets.all(24),
        child: ConstrainedBox(
          constraints: const BoxConstraints(maxWidth: 440),
          child: Card(
            child: Padding(
              padding: const EdgeInsets.all(28),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Icon(
                    Icons.apartment,
                    size: 56,
                    color: Color(0xff143d59),
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'CPM Core',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 30, fontWeight: FontWeight.bold),
                  ),
                  const Text(
                    'إدارة المشاريع والتسليمات الهندسية',
                    textAlign: TextAlign.center,
                  ),
                  const SizedBox(height: 32),
                  TextField(
                    controller: login,
                    decoration: const InputDecoration(
                      labelText: 'البريد الإلكتروني أو رقم الهاتف',
                    ),
                    textDirection: TextDirection.ltr,
                  ),
                  const SizedBox(height: 16),
                  TextField(
                    controller: password,
                    obscureText: obscure,
                    decoration: InputDecoration(
                      labelText: 'كلمة المرور',
                      suffixIcon: IconButton(
                        onPressed: () => setState(() => obscure = !obscure),
                        icon: Icon(
                          obscure ? Icons.visibility : Icons.visibility_off,
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 24),
                  FilledButton(
                    onPressed: busy
                        ? null
                        : () => run(() => Api.login(login.text, password.text)),
                    child: Text(busy ? 'جارٍ تسجيل الدخول…' : 'تسجيل الدخول'),
                  ),
                  TextButton(
                    onPressed: busy
                        ? null
                        : () => Navigator.push(
                            context,
                            MaterialPageRoute(
                              builder: (_) => const RegisterPage(),
                            ),
                          ),
                    child: const Text('إنشاء حساب'),
                  ),
                  TextButton(
                    onPressed: busy
                        ? null
                        : () => run(() async {
                            if (!login.text.contains('@'))
                              throw Exception(
                                'أدخل بريدك الإلكتروني لاستعادة كلمة المرور',
                              );
                            await Api.auth.sendPasswordResetEmail(
                              email: login.text.trim(),
                            );
                            if (mounted)
                              ScaffoldMessenger.of(context).showSnackBar(
                                const SnackBar(
                                  content: Text(
                                    'تم طلب رسالة استعادة كلمة المرور',
                                  ),
                                ),
                              );
                          }),
                    child: const Text('نسيت كلمة المرور؟'),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    ),
  );
}

class RegisterPage extends StatefulWidget {
  const RegisterPage({super.key});
  @override
  State<RegisterPage> createState() => _RegisterPageState();
}

class _RegisterPageState extends State<RegisterPage> {
  final fields = {
    for (final k in [
      'name',
      'email',
      'phone',
      'address',
      'password',
      'officeName',
    ])
      k: TextEditingController(),
  };
  bool office = false, busy = false;
  @override
  void dispose() {
    for (final c in fields.values) c.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('إنشاء حساب')),
    body: Center(
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 520),
        child: ListView(
          padding: const EdgeInsets.all(24),
          children: [
            for (final e in {
              'name': 'الاسم',
              'email': 'البريد الإلكتروني',
              'phone': 'الهاتف مع رمز الدولة',
              'address': 'العنوان',
              'password': 'كلمة المرور',
            }.entries)
              Padding(
                padding: const EdgeInsets.only(bottom: 16),
                child: TextField(
                  controller: fields[e.key],
                  obscureText: e.key == 'password',
                  decoration: InputDecoration(labelText: e.value),
                ),
              ),
            SwitchListTile(
              title: const Text('تسجيل مكتب هندسي'),
              value: office,
              onChanged: busy ? null : (v) => setState(() => office = v),
            ),
            if (office)
              TextField(
                controller: fields['officeName'],
                decoration: const InputDecoration(labelText: 'اسم المكتب'),
              ),
            const SizedBox(height: 24),
            FilledButton(
              onPressed: busy
                  ? null
                  : () async {
                      setState(() => busy = true);
                      try {
                        for (final k in [
                          'name',
                          'email',
                          'phone',
                          'password',
                        ]) {
                          if (fields[k]!.text.trim().isEmpty)
                            throw Exception('أكمل الحقول المطلوبة');
                        }
                        if (office && fields['officeName']!.text.trim().isEmpty)
                          throw Exception('أدخل اسم المكتب');
                        await Api.auth.createUserWithEmailAndPassword(
                          email: fields['email']!.text.trim(),
                          password: fields['password']!.text,
                        );
                        await Api.call('provisionProfile', {
                          'name': fields['name']!.text.trim(),
                          'phone': fields['phone']!.text.trim(),
                          'address': fields['address']!.text.trim(),
                          'officeName': office
                              ? fields['officeName']!.text.trim()
                              : null,
                        });
                        if (context.mounted) Navigator.pop(context);
                      } catch (e) {
                        if (context.mounted) error(context, e);
                      } finally {
                        if (mounted) setState(() => busy = false);
                      }
                    },
              child: Text(busy ? 'جارٍ إنشاء الحساب…' : 'إنشاء الحساب'),
            ),
          ],
        ),
      ),
    ),
  );
}

class Dashboard extends StatefulWidget {
  const Dashboard({super.key});
  @override
  State<Dashboard> createState() => _DashboardState();
}

class _DashboardState extends State<Dashboard> {
  late Future<Json> future;
  @override
  void initState() {
    super.initState();
    refresh();
  }

  void refresh() {
    setState(() => future = Api.call('dashboard'));
  }

  Future<void> join() async {
    final d = await form(context, 'الانضمام إلى مشروع', {'code': 'كود الدعوة'});
    if (d == null) return;
    try {
      await Api.call('joinProject', d);
      refresh();
    } catch (e) {
      if (mounted) error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(
      title: const Text('CPM Core'),
      actions: [
        IconButton(onPressed: refresh, icon: const Icon(Icons.refresh)),
        IconButton(
          onPressed: () => Api.auth.signOut(),
          icon: const Icon(Icons.logout),
        ),
      ],
    ),
    body: FutureBuilder<Json>(
      future: future,
      builder: (context, s) {
        if (s.hasError)
          return Center(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Text(
                  'تعذر تحميل الحساب: ${s.error}',
                  textAlign: TextAlign.center,
                ),
                TextButton(
                  onPressed: refresh,
                  child: const Text('إعادة المحاولة'),
                ),
                TextButton(
                  onPressed: () async {
                    final d = await form(context, 'إكمال بيانات الحساب', {
                      'name': 'الاسم',
                      'phone': 'الهاتف',
                    });
                    if (d != null) {
                      try {
                        await Api.call('provisionProfile', d);
                        refresh();
                      } catch (e) {
                        if (context.mounted) error(context, e);
                      }
                    }
                  },
                  child: const Text('إكمال ملف حساب جديد'),
                ),
              ],
            ),
          );
        if (!s.hasData) return const Center(child: CircularProgressIndicator());
        final d = s.data!,
            office = d['office'],
            projects = d['projects'] as List;
        return ListView(
          padding: const EdgeInsets.all(20),
          children: [
            Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'مرحباً، ${d['profile']['name']}',
                      style: Theme.of(context).textTheme.headlineSmall,
                    ),
                    if (office != null) ...[
                      const SizedBox(height: 16),
                      Text(
                        office['name'],
                        style: Theme.of(context).textTheme.titleLarge,
                      ),
                      TextButton.icon(
                        onPressed: () async {
                          try {
                            await Api.upload(
                              d['profile']['officeId'],
                              logo: true,
                            );
                            refresh();
                          } catch (e) {
                            if (context.mounted) error(context, e);
                          }
                        },
                        icon: const Icon(Icons.add_photo_alternate),
                        label: const Text('إضافة شعار PNG'),
                      ),
                      if (office['logo'] != null)
                        OfficeLogo(path: office['logo']),
                      ExpansionTile(
                        title: const Text('معلومات المكتب'),
                        children: [
                          ListTile(
                            title: Text(
                              office['active'] == true && (office['expiresAt'] as num) > DateTime.now().millisecondsSinceEpoch
                                  ? 'الاشتراك فعّال'
                                  : 'الاشتراك غير فعّال — تواصل مع الإدارة',
                            ),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),
            ),
            const SizedBox(height: 20),
            Row(children: [
              Expanded(child: Card(child: Padding(padding: const EdgeInsets.all(18), child: Text('طلبات RFI دون رد\n${projects.fold<int>(0, (n,p) => n + ((p['unanswered'] ?? 0) as num).toInt())}', textAlign: TextAlign.center)))),
              Expanded(child: Card(child: Padding(padding: const EdgeInsets.all(18), child: Text('تسليمات بانتظار الاعتماد\n${projects.fold<int>(0, (n,p) => n + ((p['pending'] ?? 0) as num).toInt())}', textAlign: TextAlign.center)))),
            ]),
            Row(
              children: [
                Expanded(
                  child: Text(
                    'مشاريعي',
                    style: Theme.of(context).textTheme.titleLarge,
                  ),
                ),
                IconButton(
                  onPressed: join,
                  icon: const Icon(Icons.key),
                  tooltip: 'الانضمام بكود',
                ),
                if (office != null)
                  IconButton(
                    onPressed: () async {
                      final data = await form(context, 'مشروع جديد', {
                        'name': 'اسم المشروع',
                        'description': 'الوصف',
                        'location': 'الموقع',
                        'type': 'نوع المشروع',
                      });
                      if (data == null) return;
                      try {
                        await Api.call('createProject', data);
                        refresh();
                      } catch (e) {
                        if (context.mounted) error(context, e);
                      }
                    },
                    icon: const Icon(Icons.add),
                  ),
              ],
            ),
            if (projects.isEmpty)
              const Padding(
                padding: EdgeInsets.all(40),
                child: Text(
                  'لا توجد مشاريع بعد. أنشئ مشروعاً أو انضم بكود دعوة.',
                  textAlign: TextAlign.center,
                ),
              ),
            for (final p in projects)
              Card(
                child: ListTile(
                  leading: const Icon(Icons.domain),
                  title: Text(p['name']),
                  subtitle: Text(p['location']),
                  trailing: const Icon(Icons.chevron_left),
                  onTap: () => Navigator.push(
                    context,
                    MaterialPageRoute(builder: (_) => ProjectPage(id: p['id'])),
                  ),
                ),
              ),
            TextButton.icon(
              onPressed: () => Navigator.push(
                context,
                MaterialPageRoute(builder: (_) => const VerifyPhonePage()),
              ),
              icon: const Icon(Icons.phone),
              label: const Text('توثيق الهاتف لتسجيل الدخول به'),
            ),
          ],
        );
      },
    ),
  );
}

class VerifyPhonePage extends StatefulWidget {
  const VerifyPhonePage({super.key});
  @override
  State<VerifyPhonePage> createState() => _VerifyPhonePageState();
}

class _VerifyPhonePageState extends State<VerifyPhonePage> {
  final phone = TextEditingController(), code = TextEditingController();
  String? verificationId;
  bool busy = false;
  @override
  void dispose() {
    phone.dispose();
    code.dispose();
    super.dispose();
  }

  Future<void> link(PhoneAuthCredential credential) async {
    try {
      await Api.auth.currentUser!.linkWithCredential(credential);
      if (mounted) {
        ScaffoldMessenger.of(context)
            .showSnackBar(const SnackBar(content: Text('تم توثيق الهاتف')));
        Navigator.pop(context);
      }
    } catch (e) {
      if (mounted) error(context, e);
    }
  }

  @override
  Widget build(BuildContext context) => Scaffold(
    appBar: AppBar(title: const Text('توثيق رقم الهاتف')),
    body: ListView(
      padding: const EdgeInsets.all(24),
      children: [
        TextField(
          controller: phone,
          decoration: const InputDecoration(
            labelText: 'الهاتف الدولي، مثال +962…',
          ),
        ),
        FilledButton(
          onPressed: busy
              ? null
              : () async {
                  setState(() => busy = true);
                  try {
                    await Api.auth.verifyPhoneNumber(
                      phoneNumber: phone.text.trim(),
                      verificationCompleted: link,
                      verificationFailed: (e) {
                        if (mounted) {
                          error(context, e);
                          setState(() => busy = false);
                        }
                      },
                      codeSent: (id, token) {
                        if (mounted)
                          setState(() {
                            verificationId = id;
                            busy = false;
                          });
                      },
                      codeAutoRetrievalTimeout: (id) {
                        verificationId = id;
                      },
                    );
                  } catch (e) {
                    if (mounted) {
                      error(context, e);
                      setState(() => busy = false);
                    }
                  }
                },
          child: const Text('إرسال رمز التحقق'),
        ),
        if (verificationId != null) ...[
          TextField(
            controller: code,
            decoration: const InputDecoration(labelText: 'رمز التحقق'),
          ),
          FilledButton(
            onPressed: () => link(
              PhoneAuthProvider.credential(
                verificationId: verificationId!,
                smsCode: code.text.trim(),
              ),
            ),
            child: const Text('تأكيد'),
          ),
        ],
      ],
    ),
  );
}

class OfficeLogo extends StatefulWidget {
  final String path;
  const OfficeLogo({super.key, required this.path});
  @override State<OfficeLogo> createState() => _OfficeLogoState();
}
class _OfficeLogoState extends State<OfficeLogo> {
  late Future<Json> image;
  @override void initState() {super.initState(); image=Api.call('readFile', {'path':widget.path});}
  @override void didUpdateWidget(OfficeLogo old) {super.didUpdateWidget(old); if(old.path!=widget.path) image=Api.call('readFile', {'path':widget.path});}
  @override Widget build(BuildContext context) => FutureBuilder<Json>(future:image,builder:(c,s) => s.hasData ? Image.network(s.data!['url'],height:140,fit:BoxFit.contain,errorBuilder:(c,e,t)=>const Text('تعذر عرض الشعار')) : const SizedBox(height:100,child:Center(child:Icon(Icons.apartment,size:48))));
}
