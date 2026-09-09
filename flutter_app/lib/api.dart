import 'dart:convert';

import 'package:cloud_functions/cloud_functions.dart';
import 'package:firebase_auth/firebase_auth.dart';
import 'package:image_picker/image_picker.dart';
import 'package:file_picker/file_picker.dart';
import 'package:url_launcher/url_launcher.dart';

typedef Json = Map<String, dynamic>;

class Api {
  static final auth = FirebaseAuth.instance;
  static Future<Json> call(String name, [Json data = const {}]) async {
    final r = await FirebaseFunctions.instance.httpsCallable(name).call(data);
    return Map<String, dynamic>.from(r.data as Map);
  }

  static Future<void> login(String login, String password) async {
    if (login.contains('@')) {
      await auth.signInWithEmailAndPassword(
        email: login.trim(),
        password: password,
      );
    } else {
      final r = await call('phoneLogin', {
        'phone': login.trim(),
        'password': password,
      });
      await auth.signInWithCustomToken(r['token']);
    }
  }

  static Future<String?> upload(
    String entity, {
    bool camera = false,
    bool image = false,
    bool logo = false,
  }) async {
    List<int>? bytes;
    String? mime;
    if (camera || image) {
      final picked = await ImagePicker().pickImage(
        source: camera ? ImageSource.camera : ImageSource.gallery,
        maxWidth: 1920,
        imageQuality: 85,
      );
      if (picked == null) return null;
      bytes = await picked.readAsBytes();
      mime = picked.name.toLowerCase().endsWith('.png')
          ? 'image/png'
          : 'image/jpeg';
    } else {
      final result = await FilePicker.platform.pickFiles(
        type: FileType.custom,
        allowedExtensions: logo ? ['png'] : ['pdf', 'png', 'jpg', 'jpeg'],
        withData: true,
      );
      if (result == null) return null;
      final f = result.files.single;
      bytes = f.bytes;
      mime = f.extension == 'pdf'
          ? 'application/pdf'
          : f.extension == 'png'
          ? 'image/png'
          : 'image/jpeg';
    }
    if (bytes == null) throw Exception('تعذر قراءة الملف');
    if (bytes.length > 5 * 1024 * 1024) throw Exception('حجم الملف يتجاوز 5MB');
    final r = await call('uploadFile', {
      'entityId': entity,
      'officeLogo': logo,
      'mime': mime,
      'base64': base64Encode(bytes),
    });
    return r['path'];
  }

  static Future<void> open(String path) async {
    final r = await call('readFile', {'path': path});
    if (!await launchUrl(
      Uri.parse(r['url']),
      mode: LaunchMode.externalApplication,
    ))
      throw Exception('تعذر فتح الملف');
  }
}
