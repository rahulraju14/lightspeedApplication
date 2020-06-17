//=======================================================================//
//                                                                       //
//  Macrobject Software Code Library                                     //
//  Copyright (c) 2004-2010 Macrobject Software, All Rights Reserved     //
//  http://www.macrobject.com                                            //
//                                                                       //
//  Warning!!!                                                           //
//      The library can only be used with web help system                //
//      created by Word-2-Web Pro.                                       //
//                                                                       //
//=======================================================================//

function moHelpIndex(pages, titles, indexs, ts_extra) 
{
  this.ps = pages;
  this.ts = titles;
  this.ti = indexs;
  this.tsx= ts_extra;
  this.ls = null;
  this.lastCreated = -1;
  this.lsm = [];
  
  this.setListbox = function(listbox)
  {
    this.ls = listbox;
  }

  this.createIndex = function()
  {
    if (this.lastCreated < 0) this.ls.length = 0;
    for(var i=this.lastCreated+1; i<ti.length; i++)
    {
      this.lsm[this.lsm.length] = -1;
      var tii = ti[i];
      var psi, tsi;
      if (tii < 0) {
        tii ++;
        tii *= -1;
        tsi = ts_extra[tii];
        tii = ps_extra[tii];
      }
      else {
        tsi = this.ts[tii];
      }
      psi = this.ps[tii];
      if (psi == 'javascript:void(0)') continue;

      var o   = document.createElement("OPTION");
      this.ls[this.ls.length] = o;
      o.value = psi;
      o.innerHTML = tsi;
      o.pageIndex = tii;
      
      this.lsm[this.lsm.length-1] = this.ls.length-1;
      if (i >= this.lastCreated + 100) {
        this.lastCreated = i;
        setTimeout('hi.createIndex();', 5);
        break;
      }
    }
  }
  
  this.search = function(keyword)
  {
    if (keyword.length == 0) return;
    keyword = keyword.toLowerCase();
    for (var i= 0; i<ti.length; i++)
    {
      var title;
      var ix = this.ti[i];
      if (ix < 0) title = this.tsx[-ix-1];
      else title = this.ts[ix];
      title = title.toLowerCase();
      if (title.indexOf(keyword) == 0)
      {
        this.ls[this.lsm[i]].selected = true;
        return;
      } 
    }
  }
  
  this.keypress = function()
  {
    if (window.event.keyCode == 13)
      this.display();
  }
  
  this.display = function()
  {
    if (this.ls.selectedIndex < 0) return;
    var item = this.ls[this.ls.selectedIndex];
    var page = item.value;
    if(moTop) {
      getFrame("content").location = "topics/" + page;
      moTop.curPageIndex = item.pageIndex;
    }
  }  
}

var hi = new moHelpIndex(ps, ts, ti, ts_extra);
