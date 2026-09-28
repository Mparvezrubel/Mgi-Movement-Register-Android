package com.mgi.movementregister;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.graphics.Typeface;
import android.net.Uri;
import android.provider.Settings;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.text.DecimalFormat;
import java.util.*;

public class MainActivity extends Activity {
    private static final int PICK_RESTORE=10, CREATE_BACKUP=11, PICK_IMPORT=12;
    private File workbookFile;
    private final List<XlsxReader.Sheet> sheets=new ArrayList<>();
    private LinearLayout content;
    private TextView title;
    private final DecimalFormat num=new DecimalFormat("#,##0.##");

    @Override public void onCreate(Bundle b){ super.onCreate(b); buildUi(); ensureWorkbook(); loadWorkbook(); showDashboard(); }

    private void buildUi(){
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(getColor(com.mgi.movementregister.R.color.surface));
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(20,12,10,12); bar.setBackgroundColor(getColor(R.color.primary));
        title=new TextView(this); title.setText("MGI Movement Register"); title.setTextColor(-1); title.setTextSize(19); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); bar.addView(title,new LinearLayout.LayoutParams(0,60,1));
        Button menu=new Button(this); menu.setText("⋮"); menu.setTextSize(26); menu.setTextColor(-1); menu.setBackgroundColor(0x00000000); bar.addView(menu,new LinearLayout.LayoutParams(60,60)); menu.setOnClickListener(v->showMenu(menu));
        root.addView(bar);
        LinearLayout nav=new LinearLayout(this); nav.setPadding(8,6,8,6); nav.setBackgroundColor(-1);
        nav.addView(navButton("Dashboard",v->showDashboard())); nav.addView(navButton("Register",v->showRegisters())); nav.addView(navButton("Search",v->showSearch())); nav.addView(navButton("Backup",v->backup())); nav.addView(navButton("Restore",v->restore()));
        root.addView(nav,new LinearLayout.LayoutParams(-1,58));
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(14,12,14,20); ScrollView sv=new ScrollView(this); sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1)); setContentView(root);
    }
    private Button navButton(String s,View.OnClickListener l){ Button b=new Button(this); b.setText(s); b.setTextSize(11); b.setOnClickListener(l); b.setAllCaps(false); b.setPadding(2,0,2,0); b.setLayoutParams(new LinearLayout.LayoutParams(0,50,1)); return b; }
    private void ensureWorkbook(){ workbookFile=new File(getFilesDir(),"MGI_Movement_Register.xlsx"); if(workbookFile.exists()) return; try(InputStream in=getAssets().open("MGI_Movement_Register-2026.xlsx"); OutputStream out=new FileOutputStream(workbookFile)){ byte[] buf=new byte[8192]; int n; while((n=in.read(buf))>0) out.write(buf,0,n); }catch(Exception e){toast("মূল ফাইল কপি হয়নি: "+e.getMessage());} }
    private void loadWorkbook(){ try{sheets.clear(); sheets.addAll(XlsxReader.read(workbookFile)); title.setText("MGI Movement Register • "+sheets.size()+" sheets");}catch(Exception e){ toast("XLSX পড়তে সমস্যা: "+e.getMessage()); }}

    private void showDashboard(){
        clear(); heading("Performance Dashboard");
        long records=0; double target=0,order=0,memo=0,landing=0; int nonEmptySheets=0;
        for(XlsxReader.Sheet s:sheets){ boolean any=false; for(List<String> r:s.rows){ if(r.size()>1&&!r.get(1).trim().isEmpty()){any=true;records++; target+=d(r,3);order+=d(r,4);memo+=d(r,5);landing+=d(r,14); }} if(any) nonEmptySheets++; }
        addCard("Sheets",String.valueOf(nonEmptySheets),"মূল workbook-এর active sheets"); addCard("Records",String.valueOf(records),"রুট/দিনের এন্ট্রি"); addCard("Target",num.format(target),"মোট target value"); addCard("Today's Order",num.format(order),"মোট order value"); addCard("Total Memo",num.format(memo),"মোট memo"); addCard("Landing Value",num.format(landing),"মোট landing value");
        double achievement=target==0?0:order/target*100; addCard("Order vs Target",num.format(achievement)+"%","সমষ্টিগত performance");
        TextView info=small("Backup/Restore-এ Android file picker ব্যবহার করা হয়েছে। সেখানে Google Drive নির্বাচন করলে Drive-এ সরাসরি backup বা restore করা যাবে—আলাদা Drive API key প্রয়োজন নেই।"); content.addView(info);
    }
    private void showRegisters(){ clear(); heading("Monthly Registers"); for(int i=0;i<sheets.size();i++){ final int idx=i; XlsxReader.Sheet s=sheets.get(i); Button b=new Button(this); b.setText((i+1)+". "+s.name); b.setAllCaps(false); b.setOnClickListener(v->showSheet(idx)); content.addView(b,new LinearLayout.LayoutParams(-1,56)); } }
    private void showSheet(int idx){ clear(); XlsxReader.Sheet s=sheets.get(idx); heading(s.name); addCard("Rows",String.valueOf(s.rows.size()),"এই sheet-এর মোট XML rows");
        String[] headers={"Date","Route","Outlet","Target","Order","Memo","Strike","LPC","IMS","CIMS","CIMS%","N.Exe","Non Exe%","C.N.Exe","Landing","Landing%"};
        for(List<String> r:s.rows){ if(r.size()<2||r.get(1).trim().isEmpty()) continue; LinearLayout card=new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(12,8,12,8); card.setBackgroundColor(-1); String route=r.get(1).trim(); TextView rt=new TextView(this); rt.setText(route); rt.setTextSize(16); rt.setTypeface(Typeface.DEFAULT,Typeface.BOLD); card.addView(rt); StringBuilder sb=new StringBuilder(); for(int c=2;c<Math.min(r.size(),16);c++){String v=r.get(c); if(v!=null&&!v.isEmpty()) sb.append(headers[c]).append(": ").append(format(v)).append("  •  ");} TextView vals=small(sb.toString()); card.addView(vals); LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2); lp.setMargins(0,0,0,8); content.addView(card,lp); }
    }
    private void showSearch(){ clear(); heading("Search Route"); EditText q=new EditText(this); q.setHint("Route name লিখুন"); content.addView(q); Button b=new Button(this); b.setText("Search"); b.setOnClickListener(v->{String query=q.getText().toString().trim().toLowerCase(Locale.ROOT); showSearchResults(query);}); content.addView(b); }
    private void showSearchResults(String q){ if(q.isEmpty()){toast("Route name দিন");return;} clear(); heading("Search: "+q); int count=0; for(XlsxReader.Sheet s:sheets) for(List<String> r:s.rows){if(r.size()>1&&r.get(1).toLowerCase(Locale.ROOT).contains(q)){ count++; TextView t=small(s.name+"  →  "+r.get(1)+"\nTarget: "+format(get(r,3))+" | Order: "+format(get(r,4))+" | Memo: "+format(get(r,5))+" | Landing: "+format(get(r,14))); t.setBackgroundColor(-1); t.setPadding(12,12,12,12); content.addView(t); }} if(count==0) content.addView(small("কোনো matching route পাওয়া যায়নি।")); }
    private void showMenu(View anchor){ PopupMenu pm=new PopupMenu(this,anchor); pm.getMenu().add("Import XLSX"); pm.getMenu().add("Refresh data"); pm.getMenu().add("About"); pm.setOnMenuItemClickListener(item->{String x=item.getTitle().toString(); if(x.startsWith("Import")){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK_IMPORT);} else if(x.startsWith("Refresh")){loadWorkbook();showDashboard();} else {new AlertDialog.Builder(this).setTitle("MGI Movement Register").setMessage("Version 1.0.0\nXLSX viewer + performance dashboard + Google Drive-compatible backup/restore.\n\nGoogle Drive backup: Backup → file picker → Drive.").setPositiveButton("OK",null).show();} return true;}); pm.show(); }
    private void backup(){ Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT); i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); i.putExtra(Intent.EXTRA_TITLE,"MGI_Movement_Register_Backup.xlsx"); startActivityForResult(i,CREATE_BACKUP); }
    private void restore(){ Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT); i.setType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"); i.addCategory(Intent.CATEGORY_OPENABLE); startActivityForResult(i,PICK_RESTORE); }
    @Override protected void onActivityResult(int req,int res,Intent data){super.onActivityResult(req,res,data); if(res!=RESULT_OK||data==null||data.getData()==null)return; Uri u=data.getData(); try{ if(req==CREATE_BACKUP) copy(new FileInputStream(workbookFile),getContentResolver().openOutputStream(u)); else { File tmp=new File(getFilesDir(),"restore.tmp.xlsx"); copy(getContentResolver().openInputStream(u),new FileOutputStream(tmp)); if(req==PICK_IMPORT||req==PICK_RESTORE){ if(tmp.length()<1000) throw new IOException("ফাইলটি XLSX মনে হচ্ছে না"); XlsxReader.read(tmp); copy(new FileInputStream(tmp),new FileOutputStream(workbookFile)); tmp.delete(); loadWorkbook(); showDashboard(); toast("Workbook সফলভাবে আপডেট হয়েছে"); }} }catch(Exception e){toast("Operation failed: "+e.getMessage());}}
    private void copy(InputStream in,OutputStream out)throws IOException{try(InputStream a=in;OutputStream b=out){byte[] buf=new byte[8192];int n;while((n=a.read(buf))>0)b.write(buf,0,n);}}
    @Override public void onBackPressed(){new AlertDialog.Builder(this).setTitle("App থেকে বের হবেন?").setMessage("আপনার বর্তমান workbook নিরাপদে সংরক্ষিত আছে।").setNegativeButton("না",null).setPositiveButton("হ্যাঁ",(d,w)->finish()).show();}
    private void clear(){content.removeAllViews();}
    private void heading(String s){TextView h=new TextView(this);h.setText(s);h.setTextSize(22);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setTextColor(getColor(R.color.text_primary));h.setPadding(2,4,2,14);content.addView(h);}
    private void addCard(String a,String b,String c){LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(16,12,16,12);box.setBackgroundColor(-1);TextView x=small(a);x.setTextSize(13);box.addView(x);TextView y=new TextView(this);y.setText(b);y.setTextSize(24);y.setTypeface(Typeface.DEFAULT,Typeface.BOLD);y.setTextColor(getColor(R.color.primary));box.addView(y);TextView z=small(c);box.addView(z);LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);lp.setMargins(0,0,0,8);content.addView(box,lp);}
    private TextView small(String s){TextView t=new TextView(this);t.setText(s);t.setTextSize(13);t.setTextColor(getColor(R.color.text_secondary));t.setPadding(2,2,2,2);return t;}
    private double d(List<String> r,int i){try{return Double.parseDouble(get(r,i));}catch(Exception e){return 0;}}
    private String get(List<String> r,int i){return i<r.size()?r.get(i):"";}
    private String format(String v){try{double x=Double.parseDouble(v);return num.format(x);}catch(Exception e){return v;}}
    private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
}
