package com.example.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.Account;
import com.example.entity.dto.StoreImage;
import com.example.mapper.AccountMapper;
import com.example.mapper.StoreImageMapper;
import com.example.service.ImageService;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import io.minio.*;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.utils.IOUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

@Slf4j
@Service
public class ImageServiceImpl extends ServiceImpl<StoreImageMapper, StoreImage> implements ImageService {
    public SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
    @Resource
    MinioClient client;
    @Resource
    AccountMapper mapper;
    @Resource
    FlowUtils flowUtils;

    @Override
    public String uploadIMage(MultipartFile file, int id) throws IOException {
        String key = Const.FORUM_IMAGE_COUNTER + id;
        if (!flowUtils.limitPerioCounterCheck(key, 20, 3600))
            return null;
        String imageName = UUID.randomUUID().toString().replace("-", "");
        Date date = new Date();
        imageName = "/cache/" + format.format(date) + "/" + imageName;
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            if (this.save(new StoreImage(id, imageName, date))) {
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传失败问题:" + e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String uploadAvatar(MultipartFile file, int id) throws IOException {
        String imageName = UUID.randomUUID().toString().replace("-", "");//随机名字的产生
        imageName = "/avatar/" + imageName;
        //存入到存储桶中
        PutObjectArgs args = PutObjectArgs.builder()
                .bucket("study")
                .stream(file.getInputStream(), file.getSize(), -1)
                .object(imageName)
                .build();
        try {
            client.putObject(args);
            String avatar = mapper.selectById(id).getAvatar();
            this.deleteOldAvatar(avatar);
            if (mapper.update(null, Wrappers.<Account>update().eq("id", id).set("avatar", imageName)) > 0) {
                return imageName;
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("图片上传失败问题:" + e.getMessage(), e);
            return null;
        }
    }

    private void deleteOldAvatar(String avatar) throws Exception {//删除旧头像
        if (avatar == null || avatar.isEmpty()) return;
        RemoveObjectArgs removeObjectArgs = RemoveObjectArgs.builder()
                .bucket("study")
                .object(avatar)
                .build();
        client.removeObject(removeObjectArgs);
    }

    @Override
    public void fetchImageFromMinio(OutputStream outputStream, String image) throws Exception {
        GetObjectArgs args = GetObjectArgs.builder()
                .bucket("study")
                .object(image)
                .build();
        GetObjectResponse response = client.getObject(args);
        IOUtils.copy(response, outputStream);
    }


}
