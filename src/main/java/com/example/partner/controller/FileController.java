package com.example.partner.controller;

import cn.dev33.satoken.stp.StpUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.core.util.StrUtil;
import com.example.partner.common.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;


import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.util.List;


// 文件上传

@RestController
@Slf4j
@RequestMapping("/file")
public class FileController {

    private static final String FILES_DIR = "/files/";

    @Value("${file.upload.path:}")
    private String uploadPath;
    @Value("${server.port:9090}")
    private String port;
    @Value("${file.download.ip:localhost}")
    private String downloadIp;

    @PostMapping("/upload")
    public Result upload(MultipartFile file) {
        String originalFilename = file.getOriginalFilename(); // 文件完整名称
        String fileName = FileUtil.mainName(originalFilename);// 文件主名称
        String extName = FileUtil.extName(originalFilename);// 获取文件的后缀名
        String uniFileFlag = IdUtil.fastSimpleUUID();
        String fileFullName = uniFileFlag + StrUtil.DOT + extName;
        // 封装完整的文件获取方法
        String fileUploadFile = getFileUploadPath(fileFullName); // 会在当前的工作的文件夹创建一个文件夹files  123 + . + jpg
        try {
            File uploadFile = new File(fileUploadFile);
            File parentFile = uploadFile.getParentFile();
            if(!parentFile.exists()) { // 如果父级目录不存在，也就是说files不存在，那么就创建
                parentFile.mkdir();
            }
            file.transferTo(uploadFile); // 把流文件转化为磁盘文件file.transferTo(new File(uploadPath)); // 把流文件转化为磁盘文件
        } catch (Exception e) {
            log.error("文件上传失败",e);
            return Result.error("文件上传失败");
        }
        return Result.success("http://" + downloadIp + ":" + port + "/file/download/" + fileFullName);
    }

    @GetMapping("/download/{fileFullName}")
    public void downloadFile(@PathVariable String fileFullName, @RequestParam(required = false) String loginId,
                             @RequestParam(required = false) String token,
                             HttpServletResponse response) throws IOException {
         // token校验
//        List<String> tokenList = StpUtil.getTokenValueListByLoginId(loginId);
//        if (CollUtil.isEmpty(tokenList) || !tokenList.contains(token)) {
//            return;
//        }

        String extName = FileUtil.extName(fileFullName);
        String fileUploadPath = getFileUploadPath(fileFullName);
        // 完整的文件路径：D:\知识星球\partner-back/files/1231321321321321982321.jpg
        byte[] bytes = FileUtil.readBytes(fileUploadPath);
        response.addHeader("Content-Disposition", "inline;filename=" + URLEncoder.encode(fileFullName, "UTF-8"));  // 预览
        List<String> attachmentFileExtNames = CollUtil.newArrayList("docx", "doc", "xlsx", "xls", "mp4", "mp3");
        if (attachmentFileExtNames.contains(extName)) {
            response.addHeader("Content-Disposition", "attachment;filename=" + URLEncoder.encode(fileFullName, "UTF-8"));  // 附件下载
        }
        OutputStream os = response.getOutputStream();
        os.write(bytes);
        os.flush();
        os.close();
    }

    private String getFileUploadPath(String fileFullName) {
        if (StrUtil.isBlank(uploadPath)) {
            uploadPath = System.getProperty("user.dir");
        }
        return uploadPath + FILES_DIR + fileFullName;
    }
}
