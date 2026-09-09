import {randomUUID} from 'node:crypto';
export const engineering = r => ['office','residentEngineer'].includes(r);
export function assert(ok, message='غير مسموح'){if(!ok) throw new Error(message);}
export function text(v,max=2000){assert(typeof v==='string' && v.trim().length>0 && v.length<=max,'تحقق من الحقول المطلوبة');return v.trim();}
export function member(p,uid){const m=p.members.find(m=>m.uid===uid);assert(m,'لست عضواً في المشروع');return m;}
export function projectView(p,uid){const m=member(p,uid);const out=structuredClone(p);
 out.rfi=out.rfi.filter(r=>m.role!=='contractor'||r.author===uid||r.recipient===uid);
 for(const key of ['ncr','files','minutes'])out[key]=out[key].filter(r=>engineering(m.role)||r.author===uid||['approved','closed'].includes(r.status));
 return out;
}
export function mutate(p,uid,action,d,now=Date.now()){
 const m=member(p,uid), eng=engineering(m.role), office=m.role==='office';
 const record=()=>({id:randomUUID(),author:uid,createdAt:now});
 const find=(arr,id)=>{const v=arr.find(x=>x.id===id);assert(v,'العنصر غير موجود');return v;};
 const stage=()=>find(p.stages,d.stageId);
 const item=()=>find(stage().items,d.itemId);
 const photo=()=>{const path=text(d.photo,500);assert(path.startsWith(`projects/${p.id}/${uid}/`),'الصورة غير صالحة');return path;};
 switch(action){
 case 'stage':assert(eng);assert(p.stages.length<10,'الحد الأقصى 10 مراحل');assert(p.members.some(x=>x.uid===d.responsible),'حدد مسؤول التسليم');p.stages.push({...record(),name:text(d.name,160),description:text(d.description),responsible:d.responsible,due:text(d.due,30),details:d.details||'',items:[]});break;
 case 'item':assert(eng||m.role==='contractor');assert(stage().items.length<5,'الحد الأقصى 5 بنود');stage().items.push({...record(),name:text(d.name,160),description:text(d.description),conditions:text(d.conditions),status:'notStarted',deliveries:[]});break;
 case 'start':assert(eng||m.role==='contractor');assert(item().status==='notStarted');item().status='inProgress';break;
 case 'deliver':assert(eng||m.role==='contractor');assert(['notStarted','inProgress','needsRevision'].includes(item().status),'لا يمكن إعادة التسليم في هذه الحالة');item().deliveries.push({...record(),photo:photo(),description:text(d.description)});item().status='awaitingApproval';break;
 case 'reviewDelivery':assert(eng);assert(item().status==='awaitingApproval');assert(['approved','needsRevision'].includes(d.status));item().status=d.status;item().review={by:uid,at:now,note:text(d.note),decision:d.status};break;
 case 'rfi':assert(p.members.some(x=>x.uid===d.recipient),'حدد المستلم');if(d.stageId)stage();if(d.itemId)item();p.rfi.push({...record(),subject:text(d.subject,160),description:text(d.description),recipient:d.recipient,importance:d.importance==='urgent'?'urgent':'normal',photo:d.photo?photo():null,stageId:d.stageId||null,itemId:d.itemId||null,reply:null});break;
 case 'replyRfi':{const r=find(p.rfi,d.id);assert(r.recipient===uid);assert(!r.reply,'تم الرد بالفعل');r.reply={text:text(d.reply),by:uid,at:now};break;}
 case 'ncr':p.ncr.push({...record(),description:text(d.description),violation:text(d.violation,160),photo:photo(),status:eng?'approved':'pending',procedure:''});break;
 case 'reviewNcr':{assert(eng);const n=find(p.ncr,d.id);assert(['pending','approved'].includes(n.status));assert(['approved','rejected','closed'].includes(d.status));assert(d.status!=='closed'||n.status==='approved');n.procedure=text(d.procedure);n.status=d.status;n.reviewer=uid;break;}
 case 'file':{const path=text(d.path,500);assert(path.startsWith(`projects/${p.id}/${uid}/`));p.files.push({...record(),name:text(d.name,160),path,category:text(d.category,60),status:eng?'approved':'pending'});break;}
 case 'minutes':p.minutes.push({...record(),title:text(d.title,160),date:text(d.date,30),attendees:text(d.attendees),decisions:text(d.decisions),attachment:d.attachment||null,status:eng?'approved':'pending'});break;
 case 'moderate':assert(eng);assert(['files','minutes'].includes(d.collection));assert(['approved','rejected'].includes(d.status));{const r=find(p[d.collection],d.id);assert(r.status==='pending');r.status=d.status;r.reviewer=uid;}break;
 case 'removeMember':assert(office);assert(d.uid!==uid);p.members=p.members.filter(x=>x.uid!==d.uid);break;
 default:throw new Error('عملية غير معروفة');
 }
 p.updatedAt=now;
 assert(Buffer.byteLength(JSON.stringify(p))<850000,'وصل المشروع لحد التخزين لهذه النسخة');
 return p;
}
export function join(p,user,invite,now=Date.now()){
 assert(invite.status==='active'&&invite.expiresAt>now,'الكود غير صالح أو منتهي');
 assert(!p.members.some(m=>m.uid===user.uid),'أنت عضو بالفعل');
 const limits={owner:1,contractor:3,residentEngineer:1};assert(limits[invite.role]);
 assert(p.members.filter(m=>m.role===invite.role).length<limits[invite.role],'اكتمل عدد أعضاء هذا الدور');
 p.members.push({uid:user.uid,role:invite.role,name:user.name,phone:user.phone,address:user.address||'',start:new Date(now).toISOString().slice(0,10),end:''});
 return p;
}

export function readablePaths(view){
 return new Set([
  ...view.files.map(f=>f.path),
  ...view.rfi.map(r=>r.photo),
  ...view.ncr.map(n=>n.photo),
  ...view.minutes.map(m=>m.attachment),
  ...view.stages.flatMap(s=>s.items.flatMap(i=>i.deliveries.map(d=>d.photo)))
 ].filter(Boolean));
}
