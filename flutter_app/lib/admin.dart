import 'package:flutter/material.dart';
import 'package:flutter_localizations/flutter_localizations.dart';
import 'package:firebase_core/firebase_core.dart';
import 'package:firebase_auth/firebase_auth.dart';

import 'api.dart';
import 'firebase_options.dart';
import 'main.dart' show error, form;

Future<void> main() async {
  WidgetsFlutterBinding.ensureInitialized();
  const webId = String.fromEnvironment('FIREBASE_WEB_APP_ID');
  if (webId.isEmpty) {
    runApp(
      const MaterialApp(
        home: Scaffold(
          body: Center(child: Text('إعداد تطبيق الويب غير مكتمل')),
        ),
      ),
    );
    return;
  }
  await Firebase.initializeApp(
    options: FirebaseOptions(
      apiKey: androidFirebaseOptions.apiKey,
      appId: webId,
      messagingSenderId: androidFirebaseOptions.messagingSenderId,
      projectId: androidFirebaseOptions.projectId,
      authDomain: 'cpm-core.firebaseapp.com',
      storageBucket: androidFirebaseOptions.storageBucket,
    ),
  );
  runApp(const AdminApp());
}

class AdminApp extends StatelessWidget {
  const AdminApp({super.key});
  @override
  Widget build(BuildContext c) => MaterialApp(
    debugShowCheckedModeBanner: false,
    locale: const Locale('ar'),
    supportedLocales: const [Locale('ar')],
    localizationsDelegates: GlobalMaterialLocalizations.delegates,
    theme: ThemeData(
      useMaterial3: true,
      colorSchemeSeed: const Color(0xff143d59),
    ),
    home: StreamBuilder<User?>(
      stream: Api.auth.authStateChanges(),
      builder: (c, s) =>
          s.data == null ? const AdminLogin() : const OfficeAdmin(),
    ),
  );
}

class AdminLogin extends StatefulWidget {
  const AdminLogin({super.key});
  @override
  State<AdminLogin> createState() => _AdminLoginState();
}

class _AdminLoginState extends State<AdminLogin> {
  final email = TextEditingController(), password = TextEditingController();
  bool busy = false;
  @override
  void dispose() {
    email.dispose();
    password.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext c) => Scaffold(
    appBar: AppBar(title: const Text('CPM Core — إدارة النظام')),
    body: Center(
      child: SizedBox(
        width: 400,
        child: ListView(
          shrinkWrap: true,
          padding: const EdgeInsets.all(24),
          children: [
            TextField(
              controller: email,
              decoration: const InputDecoration(labelText: 'البريد الإلكتروني'),
            ),
            TextField(
              controller: password,
              obscureText: true,
              decoration: const InputDecoration(labelText: 'كلمة المرور'),
            ),
            FilledButton(
              onPressed: busy
                  ? null
                  : () async {
                      setState(() => busy = true);
                      try {
                        await Api.login(email.text, password.text);
                      } catch (e) {
                        if (mounted) error(context, e);
                      } finally {
                        if (mounted) setState(() => busy = false);
                      }
                    },
              child: const Text('دخول الإدارة'),
            ),
          ],
        ),
      ),
    ),
  );
}

class OfficeAdmin extends StatefulWidget {
  const OfficeAdmin({super.key});
  @override
  State<OfficeAdmin> createState() => _OfficeAdminState();
}

class _OfficeAdminState extends State<OfficeAdmin> {
  late Future<Json> data;
  @override
  void initState() {
    super.initState();
    refresh();
  }

  void refresh() {
    setState(() {
      data = Api.call('adminOffices');
    });
  }

  Future<void> createOfficeCode() async {
    final d = await form(context, 'إنشاء مكتب وكود تفعيل', {
      'name': 'اسم المكتب الهندسي',
      'date': 'تاريخ انتهاء الاشتراك YYYY-MM-DD',
    });
    if (d == null) return;
    try {
      final date = DateTime.parse(d['date']!);
      if (!date.isAfter(DateTime.now())) {
        throw Exception('اختر تاريخاً مستقبلياً');
      }
      final result = await Api.call('createOfficeCode', {
        'name': d['name'],
        'expiresAt': date.millisecondsSinceEpoch,
      });
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (c) => AlertDialog(
          title: const Text('تم إنشاء كود المكتب'),
          content: SelectableText(
            result['code'],
            textAlign: TextAlign.center,
            style: const TextStyle(fontSize: 34, fontWeight: FontWeight.bold),
          ),
          actions: [
            FilledButton(
              onPressed: () => Navigator.pop(c),
              child: const Text('تم'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) error(context, e);
    }
  }

  Future<void> activateSubscription(dynamic office) async {
    final d = await form(context, 'تفعيل الاشتراك', {
      'date': 'تاريخ الانتهاء YYYY-MM-DD',
    });
    if (d == null) return;
    try {
      final date = DateTime.parse(d['date']!);
      if (!date.isAfter(DateTime.now()))
        throw Exception('اختر تاريخاً مستقبلياً');
      await Api.call('setSubscription', {
        'officeId': office['id'],
        'active': true,
        'expiresAt': date.millisecondsSinceEpoch,
      });
      if (mounted) refresh();
    } catch (e) {
      if (mounted) error(context, e);
    }
  }

  Future<void> stop(dynamic office) async {
    final yes = await showDialog<bool>(
      context: context,
      builder: (c) => AlertDialog(
        title: const Text('إيقاف الاشتراك؟'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(c, false),
            child: const Text('إلغاء'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(c, true),
            child: const Text('إيقاف'),
          ),
        ],
      ),
    );
    if (yes != true) return;
    try {
      await Api.call('setSubscription', {
        'officeId': office['id'],
        'active': false,
        'expiresAt': office['expiresAt'],
      });
      if (mounted) refresh();
    } catch (e) {
      if (mounted) error(context, e);
    }
  }

  @override
  Widget build(BuildContext c) => Scaffold(
    appBar: AppBar(
      title: const Text('اشتراكات المكاتب'),
      actions: [
        IconButton(
          onPressed: createOfficeCode,
          tooltip: 'إنشاء مكتب وكود تفعيل',
          icon: const Icon(Icons.add_business),
        ),
        IconButton(onPressed: refresh, icon: const Icon(Icons.refresh)),
        IconButton(
          onPressed: () => Api.auth.signOut(),
          icon: const Icon(Icons.logout),
        ),
      ],
    ),
    body: FutureBuilder<Json>(
      future: data,
      builder: (c, s) {
        if (s.hasError)
          return Center(
            child: Text(
              'تعذر الوصول. تأكد أن الحساب يملك صلاحية مدير النظام.\n${s.error}',
              textAlign: TextAlign.center,
            ),
          );
        if (!s.hasData) return const Center(child: CircularProgressIndicator());
        return ListView(
          padding: const EdgeInsets.all(24),
          children: [
            for (final office in s.data!['offices'])
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(office['name']),
                      SelectableText(office['id']),
                      Wrap(
                        children: [
                          TextButton(
                            onPressed: () => activateSubscription(office),
                            child: const Text('تفعيل / تجديد'),
                          ),
                          TextButton(
                            onPressed: () => stop(office),
                            child: const Text('إيقاف'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
          ],
        );
      },
    ),
  );
}
