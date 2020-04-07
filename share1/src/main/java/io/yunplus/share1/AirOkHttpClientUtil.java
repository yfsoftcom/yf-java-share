package io.yunplus.share1;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.apache.commons.io.FilenameUtils;
import java.util.UUID;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;

public class AirOkHttpClientUtil {

    private static final MediaType XLSX = MediaType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    private static final MediaType XLS = MediaType.parse("application/vnd.ms-excel");
    private static final MediaType IMAGE = MediaType.parse("image/*");
    private static final MediaType PDF = MediaType.parse("application/pdf");
    private static final MediaType DOC = MediaType.parse("application/msword");
    private static final MediaType DOCX = MediaType.parse("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");

    static final Function<String, MediaType> getType = filename -> {
        switch(filename){
            case "xlsx":
                return XLSX;
            case "xls":
                return XLS;
            case "jpg":
                return IMAGE;
            case "gif":
                return IMAGE;
            case "png":
                return IMAGE;
            case "jpeg":
                return IMAGE;
            case "pdf":
                return PDF;
            case "doc":
                return DOC;
            case "docx":
                return DOCX;
        }
        return null;
    };


    private static int globalTimeout = 60;

    private String url, body;

    private Response rsp;

    private String rspString;

    private Map<String, String> para;

    private Consumer<String> logger = ( msg ) -> {};

    private int timeout = -1;

    private AirOkHttpClientUtil(String url){
        this.url = url;
    }

    public static void setGlobalTimeout( int timeout ){
        globalTimeout = timeout;
    }

    public static AirOkHttpClientUtil create(String url){

        AirOkHttpClientUtil instance = new AirOkHttpClientUtil(url);
        return instance;
    }

    public AirOkHttpClientUtil timeout(int timeout){
        this.timeout = timeout;
        if(this.timeout < 1){
            this.timeout = globalTimeout;
        }
        return this;
    }

    public void download(OutputStream os) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(3600, TimeUnit.SECONDS)
                .build();
        Response rsp = client.newCall(request).execute();
        os.write(rsp.body().bytes());
        os.close();
    }

    public AirOkHttpClientUtil upload(String key, File[] files) throws IOException {
        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);

        Arrays.stream(files).forEach(file -> {
            if (file != null) {
                // MediaType.parse() 里面是上传的文件类型。
                RequestBody body = RequestBody.create(getType.apply(FilenameUtils.getExtension(file.getName())), file);
                String filename = file.getName();
                // 参数分别为， 请求key ，文件名称 ， RequestBody
                requestBody.addFormDataPart(key, filename, body);
            }
        });
        RequestBody formBody = requestBody.build();
        proceedRequest(formBody);
        return this;
    }

    /**
     * 执行请求结果
     *
     * @param requestBody 请求参数
     * @return 本对象
     * @throws IOException
     */
    private AirOkHttpClientUtil proceedRequest(RequestBody requestBody) throws IOException {
        // 如果没有设置超时，使用全局的超时时间。
        if(this.timeout < 1){
            this.timeout = globalTimeout;
        }
        Request request = new Request.Builder()
                .url(url)
                .post(requestBody)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(this.timeout, TimeUnit.SECONDS)
                .build();
        Long start = System.currentTimeMillis();
        String uuid = UUID.randomUUID().toString();;
        logger.accept(String.format("POST StartAt: %tT\nRequestID: %s\nURL: %s\nRequest: %s ", new Date(), uuid, url, body));
        this.rsp = client.newCall(request).execute();
        this.rspString = this.rsp.body().string();
        logger.accept(String.format("POST EndAt: %tT\nRequestID: %s\nTotal Use: %d ms\nURL: %s\nResponse: %s ", new Date(), uuid, (System.currentTimeMillis() - start), url, this.rspString));
        return this;
    }

    public AirOkHttpClientUtil logger(Consumer<String> logger) {
        this.logger = logger;
        return this;
    }

}
