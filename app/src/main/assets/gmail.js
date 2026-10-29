/* Ruvi Gmail discovery: no backend; OAuth access token is memory-only. */
let gmailToken=null, tokenClient=null, candidates=[];
const GMAIL_SCOPE='https://www.googleapis.com/auth/gmail.readonly';
const receiptQuery='newer_than:2y ("subscription" OR "renewal" OR "recurring" OR "membership" OR "invoice" OR "payment receipt" OR "billed") -in:spam -in:trash';
const $g=id=>document.getElementById(id);
const status=message=>{$g('gmailStatus').textContent=message};
function configuredClientId(){return ($g('clientId').value||'').trim()}
function initGmail(){
  const id=configuredClientId();
  if(!/^\d+-[a-z0-9-]+\.apps\.googleusercontent\.com$/.test(id)){status('Enter a valid Google OAuth Web Client ID.');return}
  if(!window.google?.accounts?.oauth2){status('Google sign-in could not load. Connect to the internet and try again.');return}
  tokenClient=google.accounts.oauth2.initTokenClient({client_id:id,scope:GMAIL_SCOPE,callback:response=>{
    if(response.error||!response.access_token){status('Google authorisation was not completed.');return}
    gmailToken=response.access_token;
    $g('disconnect').hidden=false;
    status('Connected for this session. Scanning receipt metadata…');
    scanGmail().catch(err=>status('Scan failed: '+err.message));
  }});
  tokenClient.requestAccessToken({prompt:'consent'});
}
async function gmailGet(path){
  if(!gmailToken)throw Error('Connect Gmail first.');
  const response=await fetch('https://gmail.googleapis.com/gmail/v1/users/me/'+path,{headers:{Authorization:'Bearer '+gmailToken},cache:'no-store'});
  if(response.status===401){gmailToken=null;throw Error('Access expired. Connect Gmail again.')}
  if(!response.ok)throw Error('Gmail API error ('+response.status+'). Check API access, OAuth configuration and consent.');
  return response.json();
}
function header(message,name){return (message.payload?.headers||[]).find(h=>h.name.toLowerCase()===name)?.value||''}
function extractCandidate(message){
  const subject=header(message,'subject');const from=header(message,'from');const body=[subject,message.snippet||''].join(' ');
  const match=body.match(/(?:₹|INR\s*|Rs\.?\s*|\$|USD\s*|£|GBP\s*|€|EUR\s*)([\d,]+(?:\.\d{1,2})?)/i);
  const symbol=match?(match[0].match(/^(₹|INR|Rs)/i)?'INR':match[0].match(/^(\$|USD)/i)?'USD':match[0].match(/^(£|GBP)/i)?'GBP':'EUR'):'INR';
  const cycle=/annual|yearly|per year|12.month/i.test(body)?'yearly':/quarter|3.month/i.test(body)?'quarterly':/weekly|per week/i.test(body)?'weekly':'monthly';
  const domain=(from.match(/@([a-z0-9.-]+)/i)||[])[1]||'';
  const name=domain?domain.replace(/^(mail|email|billing|notifications|no-reply|noreply)\./,'').split('.')[0].replace(/^./,c=>c.toUpperCase()):subject.slice(0,45)||'Unknown service';
  const amount=match?Number(match[1].replace(/,/g,'')):null;
  const date=new Date(Number(message.internalDate||Date.now())).toISOString().slice(0,10);
  return {id:message.id,subject,from,name,amount,currency:symbol,cycle,date,preview:(message.snippet||'').slice(0,220)};
}
async function scanGmail(){
  candidates=[];$g('findings').replaceChildren();
  const list=await gmailGet('messages?'+new URLSearchParams({q:receiptQuery,maxResults:'60'}));
  const ids=(list.messages||[]).map(m=>m.id);
  status('Checking '+ids.length+' possible receipts…');
  for(let i=0;i<ids.length;i+=5){
    const batch=await Promise.all(ids.slice(i,i+5).map(id=>gmailGet('messages/'+encodeURIComponent(id)+'?format=metadata&metadataHeaders=Subject&metadataHeaders=From').catch(()=>null)));
    for(const message of batch){if(message){const c=extractCandidate(message);if(c.subject||c.preview)candidates.push(c)}}
  }
  renderCandidates();status('Found '+candidates.length+' possible receipts. These are not verified active subscriptions. Review each one.');
}
function renderCandidates(){
  const root=$g('findings');root.replaceChildren();
  if(!candidates.length){const p=document.createElement('p');p.className='muted';p.textContent='No candidates found yet.';root.append(p);return}
  for(const c of candidates){
    const card=document.createElement('div');card.className='card discovery';
    const info=document.createElement('div');info.className='grow';
    const title=document.createElement('strong');title.textContent=c.subject||c.name;
    const detail=document.createElement('div');detail.className='muted small';detail.textContent=c.from+' · '+c.date;
    const preview=document.createElement('p');preview.className='small';preview.textContent=c.preview;
    const note=document.createElement('div');note.className='muted small';note.textContent=c.amount?`${c.currency} ${c.amount} · suggested ${c.cycle} (verify)`:'Amount unknown — verify manually';
    info.append(title,detail,preview,note);
    const matches=typeof ruviCandidateMatches==='function'?ruviCandidateMatches(c.name):[];if(matches.length){const flag=document.createElement('p');flag.className='small';flag.style.color='#a24b00';flag.textContent='Possible existing subscription: '+matches.map(x=>x.name).join(', ');info.append(flag)}const add=document.createElement('button');add.textContent='Review';add.onclick=()=>reviewCandidate(c);
    card.append(info,add);root.append(card);
  }
}
function reviewCandidate(c){
  openForm();$g('name').value=c.name;$g('cycle').value=c.cycle;
  if(c.currency==='INR'&&c.amount!==null)$g('amount').value=c.amount;
  // Receipt date is NOT necessarily the next renewal date. Require user input.
  $g('renewal').value='';$g('method').value='';
  status('Review the service, currency, amount, billing cycle and next renewal before saving.');
}
function disconnectGmail(){
  if(gmailToken&&window.google?.accounts?.oauth2)google.accounts.oauth2.revoke(gmailToken,()=>{});
  gmailToken=null;tokenClient=null;candidates=[];renderCandidates();$g('disconnect').hidden=true;
  status('Disconnected. Session access revoked; saved subscriptions remain on this device.');
}
$g('connect').addEventListener('click',initGmail);
$g('disconnect').addEventListener('click',disconnectGmail);
