package com.rfid.base.utils;

import android.os.Environment;
import android.text.TextUtils;

import com.rfid.base.bean.TagScan;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CsvExporter {


    static String xlsFilePath = Environment.getExternalStorageDirectory() + "/ImportTag/";
//    static String xlsFilePath = Environment.getExternalStorageDirectory() + "/";
        /**
         * 导出字符串列表到CSV文件
         * @param lists2 要导出的字符串列表
         * @throws IOException 如果发生I/O错误
         */
        public static boolean exportToCsv(  ArrayList<HashMap<String, String>> lists2 ) throws IOException {
            try  {

                String  fileName = xlsFilePath + "Tag" + GetTimesyyyymmddhhmmss() + ".csv";
                File path2 = new File(xlsFilePath);

                if (!path2.exists()) {
                  boolean ismk =  path2.mkdirs();
                    System.err.println("ismk : " + ismk);
                }

                File file = new File(fileName);
                if (file!=null){
                file.createNewFile();
                }
//                FileWriter writer = new FileWriter(fileName);
//
//                for (ScanMode.InventoryTagMap item : lsTagList) {
//                    // 对需要转义的字段添加双引号
//                    if (!TextUtils.isEmpty(item.strEPC)) {
//                        System.out.println("item.strEPC : "+item.strEPC);
//                        writer.write(item.strEPC + "\n");  // 每行写入一个字符串
//                    }
//                }


                // 2. 写入CSV数据
                try (FileWriter writer = new FileWriter(fileName)) {
                    // 写入表头（可选）
                    writer.write("EPC,RSSI\n");
//                    writer.write("EPC,Data,RSSI\n");

                    String id = "";
                    // String sxl = "";
                    for (int i = 0; i < lists2.size(); i++) {
                        Set<Map.Entry<String, String>> sets = lists2.get(i).entrySet();

                        StringBuilder sb = new StringBuilder();
                        boolean isExits = false;
                        String epc = "";
                        String rssi = "";
                        for (Map.Entry<String, String> entry : sets) {

                            if (entry.getKey().equals("tagUii")) {
                                id = entry.getValue().toString();

//                        for (int i = 0; i < row.length; i++) {
//                            sb.append(escapeCsvField(row[i]));
//                            if (i < row.length - 1) sb.append(",");
//                        }
                                epc = escapeCsvField(id);
                                isExits = true;
                            }
                            else if (entry.getKey().equals("tagRssi")) {
                                id = entry.getValue().toString();
                                rssi = escapeCsvField(id);
                            }
                            // Object value=entry.getValue();
                        }
                        if (isExits){
                            sb.append(epc);
                            if (!TextUtils.isEmpty(rssi)){
                                sb.append(","+rssi);
                            }
                            sb.append("\n");
                            writer.write(sb.toString());
                        }
                    }


//                    // 写入数据行
//                    for (ScanMode.InventoryTagMap item : lsTagList) {
//                        StringBuilder sb = new StringBuilder();
////                        for (int i = 0; i < row.length; i++) {
////                            sb.append(escapeCsvField(row[i]));
////                            if (i < row.length - 1) sb.append(",");
////                        }
//                        sb.append(escapeCsvField(item.strEPC));
//                        sb.append("\n");
//                        writer.write(sb.toString());
//                    }
                }
                System.out.println("导出成功！");
                return true;
            }catch (Exception e){
                e.printStackTrace();
                System.err.println("导出失败: " + e.getMessage());
                return false;
            }
        }


    public static boolean exportToCsvData(  List<TagScan> lists2) {
        try  {

            String  fileName = xlsFilePath + "Tag" + GetTimesyyyymmddhhmmss() + ".csv";
            File path2 = new File(xlsFilePath);

            if (!path2.exists()) {
                boolean ismk =  path2.mkdirs();
                System.err.println("ismk : " + ismk);
            }

            File file = new File(fileName);
            if (file!=null){
                file.createNewFile();
            }
            // 2. 写入CSV数据
            try (FileWriter writer = new FileWriter(fileName)) {
                // 写入表头（可选）
                writer.write("EPC,RSSI\n");
//                    writer.write("EPC,Data,RSSI\n");

                String id = "";
                // String sxl = "";

                for (int i = 0; i < lists2.size(); i++) {
                    String epc = lists2.get(i).getEpc();
                    String rssi = lists2.get(i).getRssi();

                    StringBuilder sb = new StringBuilder();
                    sb.append(epc);
                    if (!TextUtils.isEmpty(rssi)){
                        sb.append(","+rssi);
                    }
                    sb.append("\n");
                    writer.write(sb.toString());


                }
            }
            System.out.println("导出成功！");
            return true;
        }catch (Exception e){
            e.printStackTrace();
            System.err.println("导出失败: " + e.getMessage());
            return false;
        }
    }

    // 处理CSV特殊字符（逗号/引号/换行）
    private static String escapeCsvField(String field) {
        if (field == null) return "";
        if (field.contains(",") || field.contains("\"") || field.contains("\n")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }
    public static String GetTimesyyyymmddhhmmss() {

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        Date curDate = new Date(System.currentTimeMillis());// 获取当前时间
        String dt = formatter.format(curDate);

        return dt;

    }
        // 示例用法
        public static void mainTest( ) {
            // 创建示例数据
            List<String> sampleData = new ArrayList<>();
            sampleData.add("姓名");
            sampleData.add("年龄,身高");       // 包含逗号
            sampleData.add("地址\"中国\"");    // 包含双引号
            sampleData.add("备注\n第二行");    // 包含换行符

//            try {
//                // 导出到文件（注意：Android需申请存储权限）
//                exportToCsv(sampleData, "/sdcard/data.csv");
//                System.out.println("导出成功！");
//            } catch (IOException e) {
//                System.err.println("导出失败: " + e.getMessage());
//            }
        }

}
