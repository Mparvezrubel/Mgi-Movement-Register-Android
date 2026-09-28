package com.mgi.movementregister;

import org.w3c.dom.*;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

final class XlsxReader {
    static final class Sheet { final String name; final List<List<String>> rows; Sheet(String n,List<List<String>> r){name=n;rows=r;} }

    static List<Sheet> read(File file) throws Exception {
        try (ZipFile zip = new ZipFile(file)) {
            List<String> shared = readSharedStrings(zip);
            Document wb = xml(zip, "xl/workbook.xml");
            Document rel = xml(zip, "xl/_rels/workbook.xml.rels");
            Map<String,String> relMap = new HashMap<>();
            NodeList rels = rel.getElementsByTagNameNS("*", "Relationship");
            for (int i=0;i<rels.getLength();i++) {
                Element e=(Element)rels.item(i);
                relMap.put(e.getAttribute("Id"), e.getAttribute("Target"));
            }
            List<Sheet> out=new ArrayList<>();
            NodeList sheets=wb.getElementsByTagNameNS("*","sheet");
            for(int i=0;i<sheets.getLength();i++){
                Element s=(Element)sheets.item(i);
                String name=s.getAttribute("name");
                String rid=s.getAttributeNS("http://schemas.openxmlformats.org/officeDocument/2006/relationships","id");
                if(rid.isEmpty()) rid=s.getAttribute("r:id");
                String target=relMap.get(rid);
                if(target==null) continue;
                if(!target.startsWith("/")) target="xl/"+target.replace("\\","/"); else target=target.substring(1);
                if(!target.startsWith("xl/")) target="xl/"+target;
                out.add(new Sheet(name, readSheet(zip,target,shared)));
            }
            return out;
        }
    }

    private static List<String> readSharedStrings(ZipFile zip) throws Exception {
        List<String> list=new ArrayList<>();
        ZipEntry e=zip.getEntry("xl/sharedStrings.xml"); if(e==null) return list;
        Document d=xml(zip,"xl/sharedStrings.xml"); NodeList sis=d.getElementsByTagNameNS("*","si");
        for(int i=0;i<sis.getLength();i++) list.add(textOf((Element)sis.item(i),"t"));
        return list;
    }

    private static List<List<String>> readSheet(ZipFile zip,String path,List<String> shared) throws Exception {
        Document d=xml(zip,path); NodeList rn=d.getElementsByTagNameNS("*","row"); List<List<String>> rows=new ArrayList<>();
        for(int i=0;i<rn.getLength();i++){
            Element row=(Element)rn.item(i); List<String> vals=new ArrayList<>(); int max=-1;
            NodeList cn=row.getElementsByTagNameNS("*","c"); Map<Integer,String> map=new HashMap<>();
            for(int j=0;j<cn.getLength();j++){
                Element c=(Element)cn.item(j); String ref=c.getAttribute("r"); int col=colIndex(ref); max=Math.max(max,col);
                String type=c.getAttribute("t"); String v=firstText(c,"v");
                if("s".equals(type) && !v.isEmpty()) { int idx=(int)Double.parseDouble(v); v=idx<shared.size()?shared.get(idx):v; }
                else if("inlineStr".equals(type)) v=textOf(c,"t");
                else if("b".equals(type)) v="1".equals(v)?"TRUE":"FALSE";
                map.put(col,v);
            }
            for(int c=0;c<=max;c++) vals.add(map.getOrDefault(c,""));
            rows.add(vals);
        }
        return rows;
    }
    private static int colIndex(String ref){ int n=0; for(char ch:ref.toCharArray()){ if(ch<'A'||ch>'Z') break; n=n*26+(ch-'A'+1); } return n-1; }
    private static Document xml(ZipFile zip,String path) throws Exception { ZipEntry e=zip.getEntry(path); if(e==null) throw new IOException("Missing XLSX entry: "+path); DocumentBuilderFactory f=DocumentBuilderFactory.newInstance(); f.setNamespaceAware(true); try { f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true); f.setFeature("http://xml.org/sax/features/external-general-entities", false); f.setFeature("http://xml.org/sax/features/external-parameter-entities", false); f.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false); } catch (Exception ignored) {} try(InputStream in=zip.getInputStream(e)){ return f.newDocumentBuilder().parse(in); } }
    private static String firstText(Element e,String tag){ NodeList n=e.getElementsByTagNameNS("*",tag); return n.getLength()==0?"":n.item(0).getTextContent(); }
    private static String textOf(Element e,String tag){ NodeList n=e.getElementsByTagNameNS("*",tag); StringBuilder b=new StringBuilder(); for(int i=0;i<n.getLength();i++) b.append(n.item(i).getTextContent()); return b.toString(); }
}
