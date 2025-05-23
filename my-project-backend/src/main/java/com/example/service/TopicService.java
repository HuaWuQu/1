package com.example.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.example.entity.dto.Interact;
import com.example.entity.dto.Topic;
import com.example.entity.dto.TopicType;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.TopicCommentVO;
import com.example.entity.vo.response.TopicDetailVo;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;

import java.util.List;

public interface TopicService extends IService<Topic> {
    List<TopicType> listTypes();
    String createTopic(int uid, TopicCreateVO vo);
    List<TopicPreviewVO> listTopicByPage(int page , int type);
    List<TopicTopVO> listTopTopic();
    TopicDetailVo  getTopic(int tid,int uid);
    void interact(Interact interact,boolean state);
    List<TopicPreviewVO> listTopicCollects(int uid);
    String updateTopic(int uid, TopicUpdateVO vo);
    String createComment(int uid , AddCommentVO vo);
    List<TopicCommentVO> comments(int tid , int pageNumber);



}
