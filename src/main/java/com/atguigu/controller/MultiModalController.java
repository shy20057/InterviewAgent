//package com.atguigu.controller;
//
//import com.atguigu.entity.po.Result;
//import com.atguigu.service.MultiModalService;
//import com.atguigu.utils.AliyunOSSOperator;
//import io.swagger.v3.oas.annotations.Operation;
//import io.swagger.v3.oas.annotations.tags.Tag;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.PostMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//import org.springframework.web.bind.annotation.RestController;
//import org.springframework.web.multipart.MultipartFile;
//
//@Tag(name = "多模态识别接口")
//@RestController
//@RequestMapping("/api/multimodal")
//public class MultiModalController {
//
//    @Autowired
//    private MultiModalService multiModalService;
//    @Autowired
//    private AliyunOSSOperator aliyunOSSOperator;
//
//    @Operation(summary = "处理文件内容识别")
//    @PostMapping(value = "/recognize", consumes = "multipart/form-data")
//    public String recognize(@RequestParam("file") MultipartFile file,
//                            @RequestParam(value = "prompt", defaultValue = "请详细描述这张图片的内容") String prompt) {
//
//        if (file == null || file.isEmpty()) {
//            return"请选择要上传的文件";
//        }
//
//        try {
//            // 将文件交给OSS存储管理
//            String imageUrl = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
//
//            return multiModalService.recognizeFile(imageUrl, prompt);
//        } catch (Exception e) {
//            return "处理失败: " + e.getMessage();
//        }
//    }
//
//
//
//
//    @PostMapping("/upload")
//    public Result upload(@RequestParam(value = "file", required = false) MultipartFile file) throws Exception {
//        // 检查文件是否为空
//        if (file == null || file.isEmpty()) {
//            return Result.error("请选择要上传的文件");
//        }
//
//        try {
//            // 将文件交给OSS存储管理
//            String url = aliyunOSSOperator.upload(file.getBytes(), file.getOriginalFilename());
//            return Result.success(url);
//        } catch (Exception e) {
//            return Result.error("文件上传失败: " + e.getMessage());
//        }
//    }
//}
