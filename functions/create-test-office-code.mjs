import {initializeApp} from 'firebase-admin/app';
import {getFirestore} from 'firebase-admin/firestore';
import {createHash, randomInt} from 'node:crypto';

initializeApp();
const db = getFirestore();
const name = process.argv.slice(2).join(' ').trim() || 'CPM Core Test Office';
const now = Date.now();
const expiresAt = now + 30 * 24 * 60 * 60 * 1000;

for (let attempt = 0; attempt < 100; attempt += 1) {
  const code = String(randomInt(1000, 10000));
  const id = createHash('sha256').update(code).digest('hex');
  try {
    await db.doc(`officeCodes/${id}`).create({
      name,
      expiresAt,
      status: 'active',
      createdAt: now,
      createdBy: 'cloud-shell-bootstrap',
    });
    console.log(`OFFICE_CODE=${code}`);
    console.log(`EXPIRES_AT=${new Date(expiresAt).toISOString()}`);
    process.exit(0);
  } catch (error) {
    if (error?.code !== 6 && error?.code !== 'already-exists') throw error;
  }
}
throw new Error('Could not generate a unique office code');
