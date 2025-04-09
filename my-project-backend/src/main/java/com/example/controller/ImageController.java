package com.example.controller;

import com.example.entity.RestBean;
import com.example.service.ImageService;
import com.example.utils.Const;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@RestController
@RequestMapping("/api/image")
public class ImageController {
    @Resource
    ImageService service;

    @PostMapping("/cache")
    public RestBean<String> uploadImage(@RequestParam("file") MultipartFile file, @RequestAttribute(Const.ATTR_USER_ID) int id,
                                        HttpServletResponse response) throws IOException {
        if (file.getSize() > 1920 * 1080 * 5) return RestBean.failure(400, "图片大小不能超过10MB");
        log.info("正在上传图片操作......");
        String url = service.uploadIMage(file, id);
        if (url != null) {
            log.info("图片上传成功,大小" + file.getSize());
            return RestBean.success(url);
        } else {
            response.setStatus(400);
            return RestBean.failure(400, "图片上传失败,请联系管理员");
        }

    }


    @PostMapping("/avatar")//头像上传
    public RestBean<String> uploadAvatar(@RequestParam("file") MultipartFile file, @RequestAttribute(Const.ATTR_USER_ID) int id) throws IOException {
        if (file.getSize() > 1024 * 100)
            return RestBean.failure(400, "头像不能超过100kb");
        log.info("正在上传头像操作......");
        String url = service.uploadAvatar(file, id);
        if (url != null) {
            log.info("头像上传成功,大小" + file.getSize());
            return RestBean.success(url);
        } else {
            return RestBean.failure(400, "头像上传失败，请联系管理员");
        }

    }

}
