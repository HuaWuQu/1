<script setup>

import LightCard from "@/components/LightCard.vue";
import {
    ArrowRightBold,
    Calendar,
    Clock, Collection,
    CollectionTag,
    Compass,
    Document,
    Edit,
    EditPen,
    Link, MagicStick,
    Microphone,
    Picture, Star
} from "@element-plus/icons-vue";
import Weather from "@/components/Weather.vue";
import {computed, onMounted, reactive, ref, watch} from "vue";
import {get} from "@/net";
import {ElMessage} from "element-plus";
import TopicEditor from "@/components/TopicEditor.vue";
import {useStore} from "@/store";
import axios from "axios";
import ColorDot from "@/components/ColorDot.vue";
import router from "@/router";
import TopicTag from "@/components/TopicTag.vue";
import TopicCollectList from "@/components/TopicCollectList.vue";

const store = useStore()
const userIp =reactive({
    ip:''
})
onMounted(() => {
    get('/api/forum/getIp', data => {
        userIp.ip = data;
    });
})//获取IP地址

const weather = reactive({
    location: {},
    now: {},
    hourly:[],
    success:false
})
const today = computed( () =>{
    let date =new Date()
    return `${date.getMonth() + 1} 月 ${date.getUTCDate()} 日 ${date.getUTCHours() + 8} 时 ${date.getMinutes()} 分`;
})//获取时间
navigator.geolocation.getCurrentPosition(position =>{
    const longitude = position.coords.longitude;
    const latitude = position.coords.latitude;
    get(`/api/forum/weather?longitude=${longitude}&latitude=${latitude}`,data =>{
        Object.assign(weather,data);
        weather.success = true
    })
},error =>{
    console.info(error)
    ElMessage.warning('位置信息获取超时,请检查网络设置使用非谷歌浏览器,正在使用默认地理位置')
    get(`/api/forum/weather?longitude=116.40527&latitude=39.90499`,data =>{
        Object.assign(weather,data);
        weather.success = true
    })
},{
    timeout:3000,
    enableHighAccuracy:true
})
const editor = ref(false)
const topics = reactive({
    list:[],
    type : 0,
    page :0,
    end :false,
    top: []
})
const collects = ref(false)

get('api/forum/top-topic',data =>topics.top = data)
function updateList() {
    if (topics.end) return
    get(`/api/forum/list-topic?page=${topics.page}&type=${topics.type}`, data => {
        if (data){
            data.forEach(d =>topics.list.push(d))
            topics.page++
        }
      if (!data ||data.length < 10)
          topics.end = true
    })

}
watch(()=>topics.type,()=>{
    resetList()
}, {immediate:true})

function onTopicCreate(){
    editor.value = false
    resetList()
}
function resetList() {
    topics.page = 0
    topics.end =false
    topics.list = []
    updateList()
}
</script>

<template>
    <div style="display: flex;margin: 20px auto;gap: 20px;max-width: 900px;">
        <div style="flex: 1">
            <LightCard>
                <div class="create-topic" @click="editor = true">
                    <el-icon><EditPen/></el-icon>    点击发表主题
                </div>
                <div style="margin-top: 10px;display: flex;gap: 13px;font-size: 18px;color: grey">
                    <el-icon><Edit/></el-icon>
                    <el-icon><Document/></el-icon>
                    <el-icon><Compass/></el-icon>
                    <el-icon><Picture/></el-icon>
                    <el-icon><Microphone/></el-icon>
                </div>
            </LightCard>
            <LightCard style="margin-top: 10px;display: flex;flex-direction:column;gap: 10px">
                <div v-for="item in topics.top" class="top-topic" @click="router.push(`/index/topic-detail/${item.id}`)">
                    <el-tag type="info" size="small">置顶</el-tag>
                    <div>{{item.title}}</div>
                    <div>{{new Date(item.time).toLocaleDateString()}}</div>
                </div>
            </LightCard>
            <LightCard style="margin-top: 10px;display: flex;gap: 7px">
                <div :class="`type-select-card ${topics.type === item.id ? 'active' : ''}`"
                     v-for="item in store.forum.types" @click="topics.type = item.id">
                    <color-dot :color="item.color"/>
                    <span style="margin-left: 3px">{{item.name}}</span>
                </div>
            </LightCard>
            <transition name="el-fade-in" mode="out-in">
                <div v-if="topics.list.length">
                    <div style="margin-top: 10px;display: flex;flex-direction: column;gap: 10px" v-infinite-scroll="updateList" v-if="store.forum.types">
                            <LightCard v-for="item in topics.list" class="topic-card" @click="router.push('/index/topic-detail/'+item.id)">
                                <div style="display: flex">
                                    <div>
                                        <el-avatar :size="30" :src="`${axios.defaults.baseURL}/images${item.avatar}`"/>
                                    </div>
                                    <div>
                                        <div style="margin-left: 6px;transform: translateY(-2px)">
                                            <div style="font-size: 15px ;font-weight: bold">{{item.username}}</div>
                                            <div style="font-size: 12px;color: #737373">
                                                <el-icon><Clock/></el-icon>
                                                <div style="margin-left: 2px;display: inline-block;transform: translateY(-2px)">{{new Date(item.time).toLocaleString() }}</div>
                                            </div>
                                        </div>
                                    </div>
                                </div>

                                <div>
                                    <TopicTag :type="item.type"/>
                                    <span style="font-size:18px ;font-weight: bold;margin-left: 5px">{{item.title}}</span>
                                </div>
                                <div class="topic-content">{{item.text}}</div>
                                <div  style="display:grid;grid-template-columns: repeat(3,1fr);grid-gap: 10px">
                                    <el-image class="topic-image" v-for="img in item.images" :src="img" fit="cover"></el-image>
                                </div>
                                <div style="display: flex;gap: 20px;font-size: 13px;margin-top: 10px;opacity: 0.7">
                                    <div>
                                        <el-icon style="vertical-align: middle"><MagicStick/></el-icon>{{item.like}}点赞
                                    </div>
                                    <div>
                                        <el-icon style="vertical-align: middle"><Star/></el-icon>{{item.collect}}收藏
                                    </div>
                                </div>
                            </LightCard>
                    </div>
                </div>
            </transition>
        </div>
        <div style="width: 300px">
           <div style="position: sticky ;top: 20px">
               <light-card>
                   <div class="collect-list-button" @click="collects = true">
                        <span><el-icon><Collection /></el-icon>查看我的收藏</span>
                        <el-icon style="transform: translateY(3px)"><ArrowRightBold/></el-icon>
                   </div>
               </light-card>
               <LightCard style="margin-top: 20px">
                   <div style="font-weight: bold">
                       <el-icon><CollectionTag/></el-icon>
                       论坛公告
                   </div>
                   <el-divider style="margin: 10px 0" />
                   <div style="font-size: 15px;margin: 10px;color: grey">
                       这里是论坛公告内容
                   </div>
               </LightCard>
               <LightCard style="margin-top: 10px">
                   <div style="font-weight: bold">
                       <el-icon><Calendar/></el-icon>
                        天气信息
                   </div>
                   <el-divider style="margin: 10px 0" />
                   <Weather :data="weather"/>
               </LightCard>
               <LightCard>
                   <div style="display: flex;justify-content: space-between;color: gray;font-size: 14px">
                       <div style="color: black">当前日期</div>
                       <div >{{today}}</div>
                   </div>
                   <div style="display: flex;justify-content: space-between;color: gray;font-size: 14px">
                       <div style="color: black">当前ip地址</div>
                       <div >{{userIp.ip}}</div>
                   </div>
               </LightCard>
               <div style="font-size: 14px ;margin-top: 10px ;color: gray">
                   <el-icon><Link/></el-icon>友情链接
                   <el-divider style="margin: 10px 0"/>
               </div>
               <a href="https://www.ynjgy.edu.cn/" target="_blank" >
                   <el-image src="https://www.ynjgy.edu.cn/tp/pc/skin1/index/images/logo.png" style="width: 300px"></el-image>
               </a>
               <div style="display: grid;grid-template-columns: repeat(2,1fr);grid-gap: 10px;margin-top: 10px">
                   <div class="friend-link">

                       <a href="https://geek-docs.com/"  target="_blank" style="height: 50px;width: 150px">
                           <el-image  src="https://geek-docs.com/static/logo.png"/>
                       </a>
                   </div>
                   <div class="friend-link">
                       <a href="https://www.mosoteach.cn/web/index.php?c=passport"  target="_blank" style="width: 150px;height: 100px">
                           <el-image src="https://static-cdn-oss.mosoteach.cn/mosoteach2/common/images/logo-1.png?v=20190625"/>
                       </a>
                   </div>
                   <div class="friend-link">
                       <a href="https://sider.ai/zh-CN"  target="_blank" style="width: 150px ;height: 100px">
                           <el-image src="https://sider.ai/_next/static/media/logoWithText.dd97dd07.svg"/>
                       </a>
                   </div>
                   <div class="friend-link">
                       <a href="https://soft.3dmgame.com/"  target="_blank" style="width: 150px ;height: 100px">
                           <el-image src="https://www.3dmgame.com/newpage/images/logo.png"/>
                       </a>
                   </div>
               </div>
           </div>
        </div>
        <TopicEditor :show="editor" @success="onTopicCreate" @close="editor = false"/>
        <topic-collect-list :show="collects" @close="collects = false"/>
    </div>
</template>

<style lang="less" scoped>
    .collect-list-button{
        font-size: 14px;
        display: flex;
        justify-content: space-between;
        transition: .3s;
        &:hover{
            cursor: pointer;
            opacity: 0.6;
        }
    }
    .top-topic{
        display: flex;
        div:first-of-type{
            font-size: 14px;
            margin-left: 10px;
            font-weight: bold;
            opacity: 0.7;
            transition: color .3s;
            &:hover{
                color: red;
            }
        }
        div:nth-of-type(2){
            flex: 1;
            color: grey;
            font-size: 13px;
            text-align: right;
        }
        &:hover{
            cursor: pointer;
        }
    }

    .type-select-card{
        background-color: #f7f8fa;
        padding: 2px 7px;
        font-size: 15px;
        border-radius: 3px;
        box-sizing: border-box;
        transition: background-color .3s;
        &.active{
            border: solid 1px #53a853;
        }
        &:hover{
            cursor: pointer;
            background-color: #dadada;

        }
    }
    .topic-card{
        padding: 10px;
        transition: scale .3s;
        &:hover{
            scale: 1.05;
            cursor: pointer;
        }
        .topic-content{
            font-size: 13px;
            color: #676666;
            margin: 5px 0;
            display:  -webkit-box;
            -webkit-box-orient: vertical;
            -webkit-line-clamp: 3;
            overflow: hidden;
            text-overflow: ellipsis;
        }
        .topic-image{
            width: 100%;
            height: 100%;
            max-height: 120px;
            border-radius: 5px;
        }
    }
    .friend-link{
        border-radius: 5px;
        overflow: hidden;
    }
    .create-topic{
        background-color: #e8e8e8;
        border-radius: 5px;
        height: 40px;
        font-size: 14px;
        line-height: 40px;
        padding: 0 10px;
        margin: 10px;
        &:hover{
            cursor: pointer;
        }
    }
</style>