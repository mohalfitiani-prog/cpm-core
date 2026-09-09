// Run in an authenticated administrative environment, such as Cloud Shell.
import {createRequire} from 'node:module';
const require=createRequire(new URL('../functions/package.json',import.meta.url));
const {initializeApp}=require('firebase-admin/app');
const {getAuth}=require('firebase-admin/auth');
const uid=process.argv[2];
if(!uid){console.error('Usage: node tools/provision-admin.mjs FIREBASE_USER_UID');process.exit(1);}
initializeApp({projectId:'cpm-core'});
const user=await getAuth().getUser(uid);
await getAuth().setCustomUserClaims(uid,{...user.customClaims,superAdmin:true});
console.log('Administrator provisioned. Sign out and sign in again.');
