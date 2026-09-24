from pathlib import Path

p = Path('app/src/main/assets/index.html')
s = p.read_text(encoding='utf-8')

s = s.replace(
    '.btn{border:0;border-radius:12px;padding:9px 11px;font-size:12px;font-weight:800;background:#eee8df;color:var(--ink)}',
    '.btn{border:0;border-radius:12px;padding:9px 11px;font-size:12px;font-weight:800;background:#eee8df;color:var(--ink);text-decoration:none;display:inline-flex;align-items:center;justify-content:center}'
)
s = s.replace(
    '常用课程、音频、B站/抖音分享链接都可以存这里。含网址的分享文案会自动提取链接。',
    '常用课程、音频、B站/抖音链接都可以存这里。链接请直接粘贴以 http:// 或 https:// 开头的纯链接。'
)
s = s.replace(
    '<div class="field"><label>链接或分享文本</label><textarea id="matLink" placeholder="可粘贴完整分享文案，自动提取 http/https 链接"></textarea></div>',
    '<div class="field"><label>链接（可选）</label><textarea id="matLink" placeholder="例如：https://b23.tv/MRKyPie"></textarea></div>'
)
s = s.replace(
    '<div class="field"><label>材料名称</label><input id="libName"/></div><div class="field"><label>链接或分享文本</label><textarea id="libLink"></textarea></div><div class="field"><label>备注</label><textarea id="libNote"></textarea></div>',
    '<div class="field"><label>材料名称</label><input id="libName"/></div><div class="field"><label>链接（可选）</label><textarea id="libLink" placeholder="例如：https://b23.tv/MRKyPie"></textarea></div><div class="field"><label>备注</label><textarea id="libNote"></textarea></div>'
)

old = "function extractLink(s){const m=String(s||'').match(/https?:\\/\\/[^\\s<>\"']+/i);return m?m[0].replace(/[，。！？、）)\\]}]+$/,''):''}\nfunction openLink(raw){const u=extractLink(raw)||String(raw||'').trim();if(!u)return alert('这条材料没有可打开的链接');try{if(window.Android&&Android.openExternal)Android.openExternal(u);else window.open(u,'_blank')}catch(e){location.href=u}}"
new = "function normalizeLink(s){const u=String(s||'').trim();if(!u)return '';if(!/^https?:\\/\\/[^\\s]+$/i.test(u))return null;return u.replace(/[，。！？、）)\\]}]+$/,'')}\nfunction escAttr(s){return esc(s).replace(/'/g,'&#39;')}"
if old not in s:
    raise SystemExit('old link functions not found')
s = s.replace(old, new)

old_task = '${x.link?`<button class="btn green" onclick="openLink(${JSON.stringify(x.link).replace(/</g,\'\\\\u003c\')})">打开材料 ↗</button>`:\'\'}'
new_task = '${x.link?`<a class="btn green" href="${escAttr(x.link)}" target="_self">打开材料 ↗</a>`:\'\'}'
if old_task not in s:
    raise SystemExit('task link button not found')
s = s.replace(old_task, new_task)

old_lib = '${x.link?`<button class="btn green" onclick="openLink(${JSON.stringify(x.link).replace(/</g,\'\\\\u003c\')})">打开链接 ↗</button>`:\'\'}'
new_lib = '${x.link?`<a class="btn green" href="${escAttr(x.link)}" target="_self">打开链接 ↗</a>`:\'\'}'
if old_lib not in s:
    raise SystemExit('library link button not found')
s = s.replace(old_lib, new_lib)

old_save = "function saveMaterial(){const id=document.getElementById('matTask').value,i=document.getElementById('matId').value,name=document.getElementById('matName').value.trim();if(!name)return alert('先填写材料名称');const o={name,link:extractLink(document.getElementById('matLink').value),note:document.getElementById('matNote').value.trim()};if(i==='')task(id).materials.push(o);else task(id).materials[+i]=o;save();closeModal('materialModal');render()}"
new_save = "function saveMaterial(){const id=document.getElementById('matTask').value,i=document.getElementById('matId').value,name=document.getElementById('matName').value.trim();if(!name)return alert('先填写材料名称');const raw=document.getElementById('matLink').value;const link=normalizeLink(raw);if(link===null)return alert('链接格式不正确，请直接粘贴以 http:// 或 https:// 开头的纯链接');const o={name,link:link||'',note:document.getElementById('matNote').value.trim()};if(i==='')task(id).materials.push(o);else task(id).materials[+i]=o;save();closeModal('materialModal');render()}"
if old_save not in s:
    raise SystemExit('saveMaterial not found')
s = s.replace(old_save, new_save)

old_libsave = "function saveLibrary(){const name=document.getElementById('libName').value.trim();if(!name)return alert('先填写材料名称');state.library.push({task:document.getElementById('libTask').value,name,link:extractLink(document.getElementById('libLink').value),note:document.getElementById('libNote').value.trim()});save();closeModal('libraryModal');render()}"
new_libsave = "function saveLibrary(){const name=document.getElementById('libName').value.trim();if(!name)return alert('先填写材料名称');const raw=document.getElementById('libLink').value;const link=normalizeLink(raw);if(link===null)return alert('链接格式不正确，请直接粘贴以 http:// 或 https:// 开头的纯链接');state.library.push({task:document.getElementById('libTask').value,name,link:link||'',note:document.getElementById('libNote').value.trim()});save();closeModal('libraryModal');render()}"
if old_libsave not in s:
    raise SystemExit('saveLibrary not found')
s = s.replace(old_libsave, new_libsave)

p.write_text(s, encoding='utf-8')
print('index.html patched for direct native URL navigation')
