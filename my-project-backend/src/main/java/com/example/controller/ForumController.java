package com.example.controller;

import com.alibaba.fastjson2.JSONObject;
import com.example.entity.RestBean;
import com.example.entity.dto.Interact;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.*;
import com.example.service.TopicService;
import com.example.service.WeatherService;
import com.example.utils.Const;
import com.example.utils.ControllerUtils;
import com.example.utils.IpUtil;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    @Resource
    WeatherService weatherService;

    @Resource
    TopicService topicService;
    @Resource
    ControllerUtils controllerUtils;

    @GetMapping("/weather")
    public RestBean<WeatherVO> weather(double longitude, double latitude) {
        WeatherVO vo = weatherService.fetchWeather(longitude, latitude);
        return vo == null ?
                RestBean.failure(400, "获取地理位置和天气失败，请联系管理员") : RestBean.success(vo);

    }

    @GetMapping("/getIp")
    @ResponseBody
    public RestBean<String> getIp(HttpServletRequest request) {
        String ip = IpUtil.getIpAddr(request);
        return RestBean.success(ip);
    }

    @GetMapping("/types")
    public RestBean<List<TopicTypeVO>> listTypes(){
        return RestBean.success(topicService
                .listTypes()
                .stream()
                .map(type ->type.asViewObject(TopicTypeVO.class))
                .toList()
        );
    }

    @PostMapping("/create-topic")
    public RestBean<Void> createTopic(@Valid @RequestBody TopicCreateVO vo , @RequestAttribute(Const.ATTR_USER_ID) int id ){
        return    controllerUtils.messageHandle(() ->topicService.createTopic(id,vo));
    }

    @GetMapping("/list-topic")
    public RestBean<List<TopicPreviewVO>> listTopic(@RequestParam @Min(0) int page,@RequestParam @Min(0) int type){
            return RestBean.success(topicService.listTopicByPage(page+1, type));
    }

    @GetMapping("/top-topic")
    public RestBean<List<TopicTopVO>> topTopic(){
        return RestBean.success(topicService.listTopTopic());
    }

    @GetMapping("/topic")
    public RestBean<TopicDetailVo> topic(@RequestParam @Min(0) int tid, @RequestAttribute(Const.ATTR_USER_ID) int id ){
            return RestBean.success(topicService.getTopic(tid,id));
    }
    @GetMapping("/interact")
    public RestBean<Void> interact(@RequestParam @Min(0) int tid,
                                   @RequestParam @Pattern(regexp = "(like|collect)")String type,
                                   @RequestParam boolean state,
                                   @RequestAttribute(Const.ATTR_USER_ID) int id){
            topicService.interact(new Interact(tid,id,new Date(),type),state);
            return RestBean.success();
    }

    @GetMapping("/collects")
    public RestBean<List<TopicPreviewVO>> collects(@RequestAttribute(Const.ATTR_USER_ID) int id){
        return RestBean.success(topicService.listTopicCollects(id));
    }

    @PostMapping("/update-topic")
    public RestBean<Void> updateTopic(@Valid @RequestBody TopicUpdateVO vo,@RequestAttribute(Const.ATTR_USER_ID) int id){
        return    controllerUtils.messageHandle(() ->topicService.updateTopic(id,vo));
    }

    @PostMapping("/add-comment")
        public RestBean<Void> addComment(@Valid @RequestBody AddCommentVO vo ,@RequestAttribute(Const.ATTR_USER_ID) int id){
            return controllerUtils.messageHandle(() -> topicService.createComment(id,vo));
    }
    @GetMapping("/comments")
    public RestBean<List<TopicCommentVO>> comments(@RequestParam @Min(0) int tid , @RequestParam @Min(0) int page){
        return RestBean.success(topicService.comments(tid ,page+1));
    }
}
