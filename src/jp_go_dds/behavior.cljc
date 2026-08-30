(ns jp-go-dds.behavior
  "Small, dependency-free browser behaviours for DADS components.

  Components remain pure hiccup in `jp-go-dds.core`; hosts that render static
  documents can publish these scripts as same-origin assets. This keeps the
  interaction contract in one place without requiring a framework runtime.")

(def language-selector-script
  "Delegated interaction for every `dds/language-selector` in a document."
  (str
   "(()=>{'use strict';"
   "const roots=()=>[...document.querySelectorAll('[data-language-selector]')];"
   "const parts=root=>({opener:root.querySelector('[data-language-selector-opener]'),popup:root.querySelector('[data-language-selector-popup]'),items:[...root.querySelectorAll('[data-language-selector-item]')]});"
   "const close=root=>{const {opener,popup}=parts(root);if(!opener||!popup)return;popup.hidden=true;opener.setAttribute('aria-expanded','false');};"
   "const open=root=>{const {opener,popup,items}=parts(root);if(!opener||!popup)return;roots().forEach(other=>{if(other!==root)close(other);});popup.hidden=false;opener.setAttribute('aria-expanded','true');(items.find(item=>item.hasAttribute('aria-current'))||items[0])?.focus();};"
   "document.addEventListener('click',event=>{const opener=event.target.closest('[data-language-selector-opener]');if(opener){const root=opener.closest('[data-language-selector]');const expanded=opener.getAttribute('aria-expanded')==='true';expanded?close(root):open(root);return;}roots().forEach(root=>{if(!root.contains(event.target))close(root);});});"
   "document.addEventListener('keydown',event=>{const root=event.target.closest('[data-language-selector]');if(!root)return;const {opener,popup,items}=parts(root);if(event.key==='Escape'){close(root);opener?.focus();return;}if(popup?.hidden||!items.length)return;const current=Math.max(0,items.indexOf(document.activeElement));let next=null;if(event.key==='ArrowDown')next=(current+1)%items.length;if(event.key==='ArrowUp')next=(current-1+items.length)%items.length;if(event.key==='Home')next=0;if(event.key==='End')next=items.length-1;if(next!==null){event.preventDefault();items[next].focus();}});"
   "})();"))
