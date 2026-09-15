import {initializeApp} from 'firebase-admin/app';
import {getAuth} from 'firebase-admin/auth';
import {getFirestore} from 'firebase-admin/firestore';
import {getStorage} from 'firebase-admin/storage';
import {onCall,HttpsError} from 'firebase-functions/v2/https';
import {defineString} from 'firebase-functions/params';
import {randomBytes,randomUUID,createHash} from 'node:crypto';
import {assert,text,member,projectView,mutate,join,readablePaths} from './domain.js';
initializeApp();
const db=getFirestore(), auth=getAuth(), apiKey=defineString('CPM_WEB_API_KEY');
const call=fn=>onCall({region:'us-central1',maxInstances:10},async req=>{try{return await fn(req);}catch(e){if(e instanceof HttpsError)throw e;throw new HttpsError('failed-precondition',e.message||'تعذر تنفيذ الطلب');}});
const uid=r=>{if(!r.auth)throw new HttpsError('unauthenticated','سجّل الدخول');return r.auth.uid;};
const hash=v=>createHash('sha256').update(v).digest('hex');
async function active(tx,officeId){const s=await tx.get(db.doc(`offices/${officeId}`));assert(s.exists&&s.data().active&&s.data().expiresAt>Date.now(),'اشتراك المكتب غير فعّال');return s.data();}
export const validateAccessCode=call(async r=>{
 const code=text(r.data.code,40).toUpperCase(),key=hash(code),now=Date.now();
 const [office,invite]=await Promise.all([db.doc(`officeCodes/${key}`).get(),db.doc(`invites/${key}`).get()]);
 const ov=office.data(),iv=invite.data();
 assert((office.exists&&ov.status==='active'&&ov.expiresAt>now)||(invite.exists&&iv.status==='active'&&iv.expiresAt>now),'الكود غير صالح أو منتهي');
 return {valid:true,type:office.exists?'office':'team'};
});

export const createOfficeCode=call(async r=>{
 uid(r);assert(r.auth.token.superAdmin===true);
 const name=text(r.data.name,160),expiresAt=Number(r.data.expiresAt);
 assert(Number.isFinite(expiresAt)&&expiresAt>Date.now(),'اختر تاريخ انتهاء مستقبلياً');
 for(let i=0;i<40;i++){
  const code=String(Math.floor(1000+Math.random()*9000)),ref=db.doc(`officeCodes/${hash(code)}`);
  const created=await db.runTransaction(async tx=>{const s=await tx.get(ref);if(s.exists)return false;tx.create(ref,{name,expiresAt,status:'active',createdAt:Date.now(),createdBy:r.auth.uid});return true;});
  if(created)return {code};
 }
 throw new Error('تعذر توليد كود فريد، أعد المحاولة');
});

export const phoneLogin=call(async r=>{
 const phone=text(r.data.phone,30),password=text(r.data.password,200);
 assert(/^\+[1-9]\d{7,14}$/.test(phone),'استخدم رقم الهاتف الدولي');
 const ref=db.doc(`loginLimits/${hash((r.rawRequest.ip||'unknown')+phone)}`);
 await db.runTransaction(async tx=>{const s=await tx.get(ref),v=s.data(),now=Date.now();const count=v&&v.until>now?v.count:0;assert(count<5,'حاول مجدداً بعد 15 دقيقة');tx.set(ref,{count:count+1,until:v&&v.until>now?v.until:now+900000});});
 try{const u=await auth.getUserByPhoneNumber(phone);assert(u.email&&!u.disabled);
 const response=await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey.value()}`,{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({email:u.email,password,returnSecureToken:true})});
 const data=await response.json();assert(response.ok&&data.localId===u.uid);return {token:await auth.createCustomToken(u.uid)};
 }catch{throw new HttpsError('unauthenticated','بيانات الدخول غير صحيحة أو الهاتف غير موثّق');}
});
export const provisionProfile=call(async r=>{
 const id=uid(r),d=r.data,ref=db.doc(`profiles/${id}`),code=d.accessCode?text(d.accessCode,40).toUpperCase():null;
 return db.runTransaction(async tx=>{const s=await tx.get(ref);if(s.exists)return s.data();
  let officeCode=null,codeRef=null;
  if(code){codeRef=db.doc(`officeCodes/${hash(code)}`);const cs=await tx.get(codeRef);if(cs.exists){officeCode=cs.data();assert(officeCode.status==='active'&&officeCode.expiresAt>Date.now(),'كود المكتب غير صالح أو مستخدم');}}
  const profile={uid:id,name:text(d.name,160),phone:text(d.phone,30),address:d.address||'',officeId:officeCode?id:null};
  tx.create(ref,profile);
  if(officeCode){tx.create(db.doc(`offices/${id}`),{name:officeCode.name,owner:id,active:true,expiresAt:officeCode.expiresAt,stoppedAt:null,logo:null});tx.update(codeRef,{status:'used',usedBy:id,usedAt:Date.now()});}
  return profile;
 });
});
export const dashboard=call(async r=>{
 const id=uid(r),profile=(await db.doc(`profiles/${id}`).get()).data();assert(profile,'أكمل إنشاء الحساب');
 const qs=await db.collection('projects').where('memberIds','array-contains',id).get();
 const projects=qs.docs.map(s=>{const p=s.data();member(p,id);const items=p.stages.flatMap(s=>s.items);const view=projectView(p,id);return {id:s.id,name:p.name,location:p.location,status:p.status,unanswered:view.rfi.filter(r=>!r.reply).length,pending:items.filter(i=>i.status==='awaitingApproval').length};});
 const office=profile.officeId?(await db.doc(`offices/${profile.officeId}`).get()).data():null;
 return {profile,office,projects};
});
export const createProject=call(async r=>{
 const id=uid(r),d=r.data,ref=db.collection('projects').doc();
 return db.runTransaction(async tx=>{const profile=(await tx.get(db.doc(`profiles/${id}`))).data();assert(profile?.officeId===id);await active(tx,id);
 const p={id:ref.id,officeId:id,name:text(d.name,160),description:text(d.description),location:text(d.location,200),type:text(d.type,100),status:'active',members:[{...profile,role:'office',start:new Date().toISOString().slice(0,10),end:''}],memberIds:[id],stages:[],rfi:[],ncr:[],files:[],minutes:[],createdAt:Date.now()};tx.create(ref,p);return {id:ref.id};});
});
export const getProject=call(async r=>{const id=uid(r),s=await db.doc(`projects/${text(r.data.projectId,100)}`).get();assert(s.exists);return projectView(s.data(),id);});
export const projectAction=call(async r=>{
 const id=uid(r),d=r.data;
 for(const path of [d.payload?.photo,d.payload?.path,d.payload?.attachment].filter(Boolean)){assert(typeof path==='string'&&path.startsWith(`projects/${d.projectId}/${id}/`),'المرفق غير صالح');const [exists]=await getStorage().bucket().file(path).exists();assert(exists,'المرفق غير موجود');}
 const ref=db.doc(`projects/${text(d.projectId,100)}`);
 return db.runTransaction(async tx=>{const s=await tx.get(ref);assert(s.exists);const p=s.data();member(p,id);await active(tx,p.officeId);mutate(p,id,d.action,d.payload||{});p.memberIds=p.members.map(m=>m.uid);tx.set(ref,p);return {ok:true};});
});
export const invite=call(async r=>{
 const id=uid(r),d=r.data,code=randomBytes(6).toString('hex').toUpperCase(),ref=db.doc(`invites/${hash(code)}`);
 await db.runTransaction(async tx=>{const s=await tx.get(db.doc(`projects/${text(d.projectId,100)}`));assert(s.exists);const p=s.data();assert(member(p,id).role==='office');await active(tx,p.officeId);assert(['owner','contractor','residentEngineer'].includes(d.role));tx.create(ref,{projectId:s.id,role:d.role,status:'active',expiresAt:Date.now()+7*86400000,createdBy:id});});return {code};
});
export const joinProject=call(async r=>{
 const id=uid(r),ref=db.doc(`invites/${hash(text(r.data.code,40).toUpperCase())}`);
 return db.runTransaction(async tx=>{const s=await tx.get(ref);assert(s.exists,'الكود غير صالح');const inv=s.data(),pr=db.doc(`projects/${inv.projectId}`);const ps=await tx.get(pr),us=await tx.get(db.doc(`profiles/${id}`));assert(ps.exists&&us.exists);const p=ps.data();await active(tx,p.officeId);join(p,us.data(),inv);p.memberIds=p.members.map(m=>m.uid);tx.set(pr,p);tx.update(ref,{status:'used',usedBy:id});return {id:pr.id};});
});
export const uploadFile=call(async r=>{
 const id=uid(r),d=r.data;const scope=d.officeLogo?'offices':'projects',entity=text(d.entityId,100);
 if(d.officeLogo){assert(entity===id);assert((await db.doc(`offices/${id}`).get()).exists);}else{const p=(await db.doc(`projects/${entity}`).get()).data();assert(p);member(p,id);await db.runTransaction(tx=>active(tx,p.officeId));}
 assert(['image/jpeg','image/png','application/pdf'].includes(d.mime),'نوع الملف غير مدعوم');if(d.officeLogo)assert(d.mime==='image/png','الشعار يجب أن يكون PNG');
 const bytes=Buffer.from(text(d.base64,7000000),'base64');assert(bytes.length<=5*1024*1024,'الحد الأقصى 5MB');
 const path=`${scope}/${entity}/${id}/${randomUUID()}`;await getStorage().bucket().file(path).save(bytes,{contentType:d.mime,resumable:false});
 if(d.officeLogo){assert(d.mime==='image/png','الشعار يجب أن يكون PNG');await db.doc(`offices/${id}`).update({logo:path});}return {path};
});
export const readFile=call(async r=>{
 const id=uid(r),path=text(r.data.path,500),parts=path.split('/');assert(parts.length===4);
 if(parts[0]==='offices'){assert(parts[1]===id&&parts[2]===id);}else{assert(parts[0]==='projects');const p=(await db.doc(`projects/${parts[1]}`).get()).data();assert(p);const view=projectView(p,id);assert(readablePaths(view).has(path),'الملف غير متاح');}
 const [url]=await getStorage().bucket().file(path).getSignedUrl({action:'read',expires:Date.now()+5*60000});return {url};
});
// Subscription provisioning is only available to an explicitly provisioned admin claim.
export const setSubscription=call(async r=>{uid(r);assert(r.auth.token.superAdmin===true);const d=r.data;assert(typeof d.active==='boolean'&&Number.isFinite(d.expiresAt));const ref=db.doc(`offices/${text(d.officeId,100)}`);await db.runTransaction(async tx=>{const s=await tx.get(ref);assert(s.exists);tx.update(ref,{active:d.active,expiresAt:d.expiresAt,stoppedAt:d.active?null:(s.data().stoppedAt||Date.now())});});return {ok:true};});

export const adminOffices=call(async r=>{uid(r);assert(r.auth.token.superAdmin===true);const qs=await db.collection('offices').limit(200).get();return {offices:qs.docs.map(s=>({id:s.id,...s.data()}))};});
