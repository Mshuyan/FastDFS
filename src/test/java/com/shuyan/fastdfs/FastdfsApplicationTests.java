package com.shuyan.fastdfs;

import com.github.tobato.fastdfs.domain.StorePath;
import com.github.tobato.fastdfs.proto.storage.DownloadByteArray;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.github.tobato.fastdfs.service.TrackerClient;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.*;

@RunWith(SpringRunner.class)
@SpringBootTest
public class FastdfsApplicationTests {
    @Autowired
    private FastFileStorageClient fastFileStorageClient;

    @Test
    public void contextLoads() {
    }

    /**
     * 上传主从文件
     */
    @Test
    public void upload() {
        File file = new File("/Users/will/Downloads/1039111.jpg");
        StorePath storePath;
        try (FileInputStream inputStream = new FileInputStream(file)) {
            // 上传主文件
            storePath = fastFileStorageClient.uploadFile(inputStream, file.length(), "jpg", null);
            System.out.println("主文件上传成功 " + storePath.getGroup() + " " + storePath.getPath());
        } catch (Exception e) {
            return;
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            // 上传从文件
            // prefixName：指定从文件的尾缀名，`.jpg`的`.`之前的内容，而不是添加在整个文件名之前；该参数不能为null或""
            // fileExtName：指定文件后缀名，必须是该文件真实的后缀名
            fastFileStorageClient.uploadSlaveFile(storePath.getGroup(), storePath.getPath(), inputStream, file.length(), "_slave", "jpg");
            System.out.println("从文件上传成功");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 上传并创建缩略图
     */
    @Test
    public void uploadCrtThumbImage() {
        File file = new File("/Users/will/Downloads/1039111.jpg");
        try (FileInputStream inputStream = new FileInputStream(file)) {
            // 测试上传原图片并自动创建缩略图
            // 缩略图尺寸在 application.properties 中定义
            // 缩略图文件名为：storePath.getPath()_宽x高.后缀名；如：CtM3BFvcF6iALYiSAAAvfv9Uu40001_130x150.jpg
            StorePath storePath = fastFileStorageClient.uploadImageAndCrtThumbImage(inputStream, file.length(), "jpg", null);
            System.out.println(storePath.getGroup() + "  " + storePath.getPath());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试文件下载
     */
    @Test
    public void download() {
        try {
            byte[] bytes = fastFileStorageClient.downloadFile("cpmall", "M00/00/00/wKiAjVlpMfiAagnbAADGA0F72jo134_150x150.jpg", new DownloadByteArray());
            FileOutputStream stream = new FileOutputStream("a.jpg");
            stream.write(bytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 测试文件删除
     * 只会删除指定的文件，不会连带删除从文件、缩略图等文件，那些需要自己手动删除
     */
    @Test
    public void deleteFile() {
        fastFileStorageClient.deleteFile("cpmall", "M00/00/00/CtM3BFvcFxGAWB6TAAAvfv9Uu40731.jpg");
    }
}
