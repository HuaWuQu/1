package com.example.service.impl;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.entity.dto.*;
import com.example.entity.vo.request.AddCommentVO;
import com.example.entity.vo.request.TopicCreateVO;
import com.example.entity.vo.request.TopicUpdateVO;
import com.example.entity.vo.response.TopicCommentVO;
import com.example.entity.vo.response.TopicDetailVo;
import com.example.entity.vo.response.TopicPreviewVO;
import com.example.entity.vo.response.TopicTopVO;
import com.example.mapper.*;
import com.example.service.TopicService;
import com.example.utils.CacheUtils;
import com.example.utils.Const;
import com.example.utils.FlowUtils;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;

@Service
public class TopicServiceImpl extends ServiceImpl<TopicMapper, Topic> implements TopicService {

  @Resource
    TopicTypeMapper mapper;

  @Resource
  FlowUtils   flowUtils;

  @Resource
  CacheUtils cacheUtils;

  @Resource
  AccountMapper accountMapper;

  @Resource
  AccountDetailsMapper accountDetailsMapper;

  @Resource
  AccountPrivacyMapper accountPrivacyMapper;

  @Resource
  StringRedisTemplate stringRedisTemplate;

  @Resource
  TopicCommentMapper commentMapper;
  private   Set<Integer> types = null;
  @PostConstruct
  private  void initTypes(){
    types = this.listTypes().stream().map(TopicType::getId).collect(Collectors.toSet());
  }

    @Override
    public List<TopicType> listTypes() {
        return mapper.selectList(null);
    }

  @Override
  public String createTopic(int uid, TopicCreateVO vo) {
    if (!textLimitCheck(vo.getContent(),5000))
      return "文字字数超过限制!";
    if (!types.contains(vo.getType()))
      return "文章类型非法!";
    String key = Const.FORUM_TOPIC_CREATE_COUNTER +uid;
    if (!flowUtils.limitPerioCounterCheck(key,3,3600))
      return "发文频繁,请稍后再试(一小时限定发文三章!)";
    //将vo里的数据拷贝到topic里
    Topic topic = new Topic();
    BeanUtils.copyProperties(vo,topic);
    topic.setContent(vo.getContent().toJSONString());
    topic.setUid(uid);
    topic.setTime(new Date());
    if (this.save(topic)){
      cacheUtils.deleteCacheParttern(Const.FORUM_TOPIC_PREVIEW_CACHE + "*");
      return null;
    }else {
      return "发布失败,请联系管理员";
    }
  }

  @Override
  public String updateTopic(int uid, TopicUpdateVO vo) {
    if (!textLimitCheck(vo.getContent(),5000))
      return "文字字数超过限制!";
    if (!types.contains(vo.getType()))
      return "文章类型非法!";
    baseMapper.update(null ,Wrappers.<Topic>update()
                    .eq("uid",uid)
                    .eq("id",vo.getId())
                    .set("title",vo.getTitle())
                    .set("content",vo.getContent().toString())
                    .set("type",vo.getType())
                      );
    return null;
  }

  @Override
  public String createComment(int uid, AddCommentVO vo) {//发表评论
      if (!textLimitCheck(JSONObject.parseObject(vo.getContent()),1000))
        return "评论内容太多！";
      String key = Const.FORUM_TOPIC_COMMENT_COUNTER +uid;
      if (!flowUtils.limitPerioCounterCheck(key,3,60))
        return "评论过于频繁，请稍后再试";
      TopicComment comment = new TopicComment();
      comment.setUid(uid);
      BeanUtils.copyProperties(vo,comment);
      comment.setTime(new Date());
      commentMapper.insert(comment);
      return null;
  }

  @Override
  public List<TopicCommentVO> comments(int tid, int pageNumber) {
    Page<TopicComment> page = Page.of(pageNumber,10);
    commentMapper.selectPage(page,Wrappers.<TopicComment>query().eq("tid",tid));
    return page.getRecords().stream().map(dto ->{
      TopicCommentVO vo = new TopicCommentVO();
      BeanUtils.copyProperties(dto,vo);
      if (dto.getQuote() > 0 ){
        JSONObject object = JSONObject.parseObject(commentMapper.selectOne(Wrappers.<TopicComment>query().eq("id",dto.getId()).orderByAsc("time")).getContent()
        );
        StringBuilder builder =new StringBuilder();
        this.shortContent(object.getJSONArray("ops"),builder ,ignore ->{});
        vo.setQuote(builder.toString());
      }
      TopicCommentVO.User user =new TopicCommentVO.User();
      this.fillUserDetailsByPrivacy(user,dto.getUid());
      vo.setUser(user);
      return  vo;
    }).toList();
  }

  @Override
  public List<TopicPreviewVO> listTopicByPage(int pageNumber, int type) {//获取帖子列表的功能
    String key = Const.FORUM_TOPIC_PREVIEW_CACHE + pageNumber +":" + type;
    List<TopicPreviewVO> list = cacheUtils.takeListFromCache(key,TopicPreviewVO.class);
    if (list != null)return  list;
    Page<Topic> page = Page.of(pageNumber,10);

    if (type == 0){
       baseMapper.selectPage(page,Wrappers.<Topic>query().orderByDesc("time"));
    }else {
      baseMapper.selectPage(page,Wrappers.<Topic>query().eq("type",type).orderByDesc("time"));
    }
    List<Topic> topics =page.getRecords();
    list =topics.stream().map(this::resolveToPreview).toList();
    cacheUtils.saveListToCache(key,list, 20);
    return list;
  }

  @Override
  public List<TopicTopVO> listTopTopic() {//置顶帖子的处理
    List<Topic> topics = baseMapper.selectList(Wrappers.<Topic>query().select("id","title","time").eq("top",1));
    return topics.stream().map(topic -> {
      TopicTopVO vo = new TopicTopVO();
      BeanUtils.copyProperties(topic,vo);
      return vo;
    }).toList();
  }

  @Override
  public TopicDetailVo getTopic(int tid,int uid) {//帖子详情页的功能
    TopicDetailVo vo = new TopicDetailVo();
    Topic topic = baseMapper.selectById(tid);
    BeanUtils.copyProperties(topic,vo);
    TopicDetailVo.Interact interact = new TopicDetailVo.Interact(
            hasInteract(tid,uid,"like"),
            hasInteract(tid ,uid,"collect")
    );
    vo.setInteract(interact);
    TopicDetailVo.User user  =new TopicDetailVo.User();
    vo.setUser(this.fillUserDetailsByPrivacy(user,topic.getUid()));
    vo.setComments(commentMapper.selectCount(Wrappers.<TopicComment>query().eq("tid",tid)));
    return vo;
  }

  @Override
  public void interact(Interact interact, boolean state) {//点赞收藏功能完善
    String type = interact.getType();
    synchronized (type.intern()){
      stringRedisTemplate.opsForHash().put(type,interact.toKey(),Boolean.toString(state));
      this.saveInteractSchedule(type);
    }
  }

  @Override
  public List<TopicPreviewVO> listTopicCollects(int uid) {

    return baseMapper.collectTopic(uid).stream().map(topic -> {
      TopicPreviewVO vo =new TopicPreviewVO();
      BeanUtils.copyProperties(topic,vo);
      return vo;
    }).toList();
  }
  private boolean hasInteract(int tid,int uid,String type){
    String key = tid + ":" + uid;
    if (stringRedisTemplate.opsForHash().hasKey(type,key))
      return Boolean.parseBoolean(stringRedisTemplate.opsForHash().entries(type).get(key).toString());
    return baseMapper.userInteractCount(tid, uid, type) > 0;
  }
  private  final  Map<String,Boolean> state = new HashMap<>();
  ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
  private void saveInteractSchedule(String type){//点赞收藏功能定时任务入库
    if (!state.getOrDefault(type,false)){
      state.put(type,true);
      service.schedule(()->{
        this.saveInteract(type);
        state.put(type,false);
      },3, TimeUnit.SECONDS);
    }
  }

  private void saveInteract(String type){//点赞收藏保存入库
    synchronized (type.intern()){
      List<Interact> check = new LinkedList<>();
      List<Interact> uncheck = new LinkedList<>();
      stringRedisTemplate.opsForHash().entries(type).forEach((k,v) -> {
        if (Boolean.parseBoolean(v.toString())){
          check.add(Interact.parseInteract(k.toString(),type));
        }else {
          uncheck.add(Interact.parseInteract(k.toString(),type));
        }
      });
      if (!check.isEmpty())
        baseMapper.addInteract(check,type);
      if (!uncheck.isEmpty())
        baseMapper.deleteInteract(uncheck,type);
      stringRedisTemplate.delete(type);
    }
  }
  private  <T> T fillUserDetailsByPrivacy( T target,int uid){//用户隐私设置的获取
    AccountDetails details= accountDetailsMapper.selectById(uid);
    Account account = accountMapper.selectById(uid);
    AccountPrivacy accountPrivacy = accountPrivacyMapper.selectById(uid);
    String[] ignores =  accountPrivacy.hiddenFields();
    BeanUtils.copyProperties(account,target,ignores);
    BeanUtils.copyProperties(details,target,ignores);
    return  target;
  }


  private  TopicPreviewVO resolveToPreview(Topic topic){
    TopicPreviewVO vo = new TopicPreviewVO();
    BeanUtils.copyProperties(accountMapper.selectById(topic.getUid()),vo);
    BeanUtils.copyProperties(topic ,vo);
    vo.setLike(baseMapper.interactCount(topic.getId(),"like"));
    vo.setCollect(baseMapper.interactCount(topic.getId(),"collect"));
    List<String> images = new ArrayList<>();
    StringBuilder previewText = new StringBuilder();
    JSONArray ops = JSONObject.parseObject(topic.getContent()).getJSONArray("ops");
    this.shortContent(ops,previewText,obj ->images.add(obj.toString()));
    vo.setText(previewText.length() > 300 ? previewText.substring(0, 300) : previewText.toString());
    vo.setImages(images);
    return  vo;
  }

  private void shortContent(JSONArray ops, StringBuilder previewText, Consumer<Object> imageHandler){
    for (Object op : ops) {
      Object insert =JSONObject.from(op).get("insert");
      if (insert instanceof  String text){
        if (previewText.length() >= 300)continue;
        previewText.append(text);
      }else  if (insert instanceof Map<?,?> map){
        Optional.ofNullable(map.get("image")).ifPresent(imageHandler);
      }
    }
  }

  private  boolean textLimitCheck(JSONObject object,int max){
    if (object == null)return  false;
    long length = 0;
    for (Object op : object.getJSONArray("ops")) {
      length += JSONObject.from(op).getString("insert").length();
      if (length >max)return false;
    }

    return  true;
  }
}
