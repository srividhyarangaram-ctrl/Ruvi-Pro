(function(){
const baseRender=render;
function updateDashboard(){
 const active=data.filter(s=>s.status!=='cancelled');const total=active.reduce((n,s)=>n+monthly(s),0), annual=total*12;
 const today=new Date();today.setHours(0,0,0,0);
 const upcoming=active.map(s=>({...s,days:Math.round((new Date(s.renewal+'T00:00:00')-today)/86400000)})).filter(s=>s.days>=0&&s.days<=30).sort((a,b)=>a.days-b.days);
 const pairs=typeof ruviDuplicatePairs==='function'?ruviDuplicatePairs(data):[];
 document.getElementById('metric-active').textContent=active.length;
 document.getElementById('metric-next').textContent=upcoming.length;
 document.getElementById('metric-risk').textContent=pairs.length;
 document.getElementById('metric-annual').textContent=money(annual);
 const renew=document.getElementById('renewal-list');renew.replaceChildren();
 if(!upcoming.length)renew.append(empty('No renewals recorded in the next 30 days.'));
 upcoming.slice(0,5).forEach(s=>{const row=document.createElement('div');row.className='dash-row';const left=document.createElement('div');const name=document.createElement('b');name.textContent=s.name;const date=document.createElement('small');date.textContent=s.days===0?'Today':s.days===1?'Tomorrow':'In '+s.days+' days';left.append(name,date);const amount=document.createElement('strong');amount.textContent=money(s.amount);row.append(left,amount);renew.append(row)});
 const categories=new Map();active.forEach(s=>{const key=s.cycle;categories.set(key,(categories.get(key)||0)+monthly(s))});
 const chart=document.getElementById('spend-chart');chart.replaceChildren();
 if(!data.length)chart.append(empty('Add a subscription to see your spending breakdown.'));
 [...categories].sort((a,b)=>b[1]-a[1]).forEach(([label,value])=>{const wrap=document.createElement('div');wrap.className='bar-item';const heading=document.createElement('div');heading.className='bar-heading';const name=document.createElement('span');name.textContent=label[0].toUpperCase()+label.slice(1)+' plans';const price=document.createElement('b');price.textContent=money(value)+'/mo';heading.append(name,price);const track=document.createElement('div');track.className='bar-track';const fill=document.createElement('div');fill.className='bar-fill';fill.style.width=(total?value/total*100:0)+'%';track.append(fill);wrap.append(heading,track);chart.append(wrap)});
 const alert=document.getElementById('insight-text');alert.textContent=pairs.length?`${pairs.length} possible duplicate pair${pairs.length===1?'':'s'} found. Review them before making cancellation decisions.`:data.length?'No obvious duplicate subscriptions detected. Ruvi checks service names, not bank charges.':'Add your subscriptions or scan Gmail receipts to discover possible savings.';
 }
 function empty(s){const el=document.createElement('p');el.className='muted small';el.textContent=s;return el}
 render=function(){baseRender();updateDashboard()};updateDashboard();
 document.querySelectorAll('[data-target]').forEach(b=>b.addEventListener('click',()=>document.getElementById(b.dataset.target)?.scrollIntoView({behavior:'smooth'})));
})();
