package io.yunplus.share1;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.io.*;
import java.math.BigDecimal;
import java.net.URL;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;


@Service
public class FooService {

    Function<BigDecimal[], BigDecimal> sumNumeric = numerics -> Arrays.stream(numerics).reduce((sum , item) -> sum.add(item) ).get();
    
    public FileInputStream getTheReportTemplate(String local, String md5, String url) throws Exception {
        Assert.notNull(local, "Local path required");
        String path = null;
        if(local.startsWith(File.separator)){
            // from the local disk with absolute path
            path = local;
        }else if(local.startsWith("classpath:")){
            // from the resource
            path = local.replaceFirst("classpath:", "");
            URL localResourceUrl = FooService.class.getClassLoader().getResource(path);
            if(localResourceUrl != null){
                path = FooService.class.getClassLoader().getResource(path).getFile();
            }else{
                path = FooService.class.getClassLoader().getResource("application.properties").getFile()
                        .replace("application.properties", path);
            }
        }
        Assert.notNull(path, String.format("Local parameter not legal: %s . should like : /home/xxx.xls or classpath:xxx.xls", local));
        File f = new File(path);
        boolean isMatched = false;
        if(f.exists()){
            String fileMd5 = FileUtil.checkumMd5(f);
            isMatched = md5.equalsIgnoreCase(fileMd5);
        }
        if(!isMatched){
            // download from the url
            AirOkHttpClientUtil.create(url).download(new FileOutputStream(f));
        }
        return new FileInputStream(f);
    }

    public boolean fillTheReport(JSONArray rules, List<Map<String, Object>> datas, FileInputStream workbook, FileOutputStream fos) throws Exception{
        List<XlsUtil.DataCell[]> dataCells = new ArrayList<>();
        for (int i = 0; i < rules.size(); i++ ){
            Map<String, XlsUtil.DataCell> cellKeyValues = new HashMap<>();
            JSONObject rule = rules.getJSONObject(i);
            JSONObject basic = rule.getJSONObject("basic");
            for (String cellKey: basic.keySet()) {
                int[] point = XlsUtil.getCellPosition(cellKey);
                JSONArray cellProperty = basic.getJSONArray(cellKey);
                cellKeyValues.put(cellKey, new XlsUtil.DataCell(
                        point[1],
                        point[0],
                        cellProperty.getString(0),
                        datas.get(i).getOrDefault(cellProperty.getString(1), 0)
                ));
            }

            JSONObject formula = rule.getJSONObject("formula");
            for (String cellKey: formula.keySet()) {
                int[] point = XlsUtil.getCellPosition(cellKey);
                JSONObject cellProperty = formula.getJSONObject(cellKey);
                List<BigDecimal> cells = cellProperty.getJSONArray("cells").stream()
                        .map(c ->
                            cellKeyValues.getOrDefault(c, new XlsUtil.DataCell()).getValue()
                        ).map(o -> {
                            if (o instanceof BigDecimal) {
                                return (BigDecimal) o;
                            }else if ( o instanceof Integer) {
                                return BigDecimal.valueOf((Integer) o);
                            }
                            return (BigDecimal) o;
                        }).collect(Collectors.toList());
                BigDecimal data = BigDecimal.ZERO;
                switch (cellProperty.getString("operate")){
                    case "SUM":
                        data = sumNumeric.apply(cells.toArray(new BigDecimal[]{}));
                        break;
                }
                cellKeyValues.put(cellKey, new XlsUtil.DataCell(
                        point[1],
                        point[0],
                        cellProperty.getString("type"),
                        data
                ));
            }
            dataCells.add(cellKeyValues.values().toArray(new XlsUtil.DataCell[]{}));
        }

        XlsUtil.fillPlaceholderByTemplateWorkbook(workbook, dataCells, fos);
        return true;
    }

    public boolean fillTheReport(String rules, List<Map<String, Object>> datas, FileInputStream workbook, FileOutputStream fos) throws Exception{
        return fillTheReport(JSON.parseArray(rules), datas, workbook, fos);
    }
}