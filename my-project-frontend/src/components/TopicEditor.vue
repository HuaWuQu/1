<script setup>
import { Document, Position} from "@element-plus/icons-vue";
import {computed, reactive, ref} from "vue";
import {Delta, Quill, QuillEditor} from "@vueup/vue-quill";
import '@vueup/vue-quill/dist/vue-quill.snow.css';
import ImageResize from "quill-image-resize-vue";
import {ImageExtend ,QuillWatch} from "quill-image-super-solution-module";
import axios from "axios";
import {accessHeader, get, post} from "@/net";
import {ElMessage} from "element-plus";
import ColorDot from "@/components/ColorDot.vue";
import {useStore} from "@/store";


const  store =useStore()
const props = defineProps({
    show: Boolean,
    defaultTitle:{
        default :'',
        type :String
    },
    defaultText:{
        default :'',
        type :String
    },
    defaultType:{
        default :null,
        type :Number
    },
    submitButton:{
        default :'立即发表主题',
        type :String
    },submit:{
    default:(editor,success)=>{
                post('/api/forum/create-topic', {
                    type :editor.type.id,
                    title:editor.title,
                    content: editor.text
                 },() =>{
                    ElMessage.success('帖子发表成功！')
                    success()
            })
    },
    type:Function
    }
})
const emit =defineEmits(['close','success'])
const refEditor = ref();
const  editor = reactive({
    type: null,
    title:'',
    text:'',
    loading :false,
    types : []
})
function initEditor() {
    if (props.defaultText)
        editor.text = new Delta(JSON.parse(props.defaultText))
    else
        refEditor.value.setContents('','user')
    editor.title=props.defaultTitle
    editor.type = findTypeById(props.defaultType)


}
function deltaToText(delta) {//统计字数
    if (!delta.ops)return ""
    let str =""
    for (let op of delta.ops) {
        str += op.insert
    }
    return str.replace(/\s/g,"")
}
const contentLength = computed(() =>deltaToText(editor.text).length)

function findTypeById(id) {
    for (let type of store.forum.types) {
        if (type.id === id)
            return type
    }
}
function submitTopic() {
    const text = deltaToText(editor.text);
    if (text.length >5000){
        ElMessage.warning('字数超过限制！')
        return
    }
    if (!editor.title){
        ElMessage.warning('请填写标题！')
        return;
    }
    if (!editor.type){
        ElMessage.warning('请选择帖子类型！')
        return;
    }
    props.submit(editor,()=>emit('success'))
}

Quill.register('modules/imageResize',ImageResize)
Quill.register('modules/ImageExtend',ImageExtend)
const editorOption ={
    modules :{
        toolbar :{
            container:[
                "bold", "italic","underline","strike","clean",
                {color:[]},{"background":[]},
                {size:["small",false,"large","huge"]},
                {header:[1,2,3,4,5,6,false]},
                {list:"ordered"},{list:"bullet"},{align:[]},
                "blockquote","code-block","link","image",
                {indent :'-1'},{indent: '+1'}
            ],
            handlers :{
                'image':function (){
                    QuillWatch.emit(this.quill.id)
                }
            }
        },
        imageResize:{
            modules: ['Resize','DisplaySize']
        },
        ImageExtend:{
            action: axios.defaults.baseURL + '/api/image/cache',
            name: 'file',
            size: 5,
            loading: true,
            accept:'image/png, image/jpeg',
            response: (resp) =>{
                if (resp.data){
                    return axios.defaults.baseURL + '/images' +resp.data
                }else {
                    return  null
                }
            },
            methods:"POST",
            headers: xhr =>{
                xhr.setRequestHeader('Authorization',accessHeader().Authorization);
            },
            start:()=>editor.uploading = true,
            success:() =>{
                ElMessage.success('图片上传成功！')
                editor.uploading =false
            },
            error:() =>{
                ElMessage.warning('图片上传失败,请联系管理员！')
                editor.uploading =false
            }


        }
    }

}

</script>

<template>
    <div>
        <el-drawer :model-value="show" :size='650' direction="btt"
                   @open="initEditor"
                   :close-on-click-modal="false" @close="emit('close')">
            <template #header>
                <div>
                    <div style="font-weight: bold">发表新的帖子</div>
                    <div style="font-size: 13px">发表帖子之前，请遵守相关规定</div>
                </div>
            </template>
            <div style="display: flex ;gap: 10px">
                <div style="width: 150px">
                    <el-select placeholder="请选择主题类型" value-key="id" v-model="editor.type" :disabled="!store.forum.types.length">
                        <el-option v-for="item in  store.forum.types.filter(type => type.id >0)" :value="item" :label="item.name">
                            <div>
                                <color-dot :color="item.color"/>
                                <span style="margin-left: 10px">{{item.name}}</span>
                            </div>
                        </el-option>
                    </el-select>
                </div>
                <div style="flex: 1 ;">
                    <el-input v-model="editor.title" placeholder="请输入帖子标题" :prefix-icon="Document"
                        style="height: 100%;" maxlength="50"
                    />
                </div>
            </div>
            <div style="margin-top: 10px; color: #1a1a1c;font-size: smaller">
                <color-dot :color="editor.type ? editor.type.color : '#8c8c8d'" />
               <span style="margin-left: 6px"> {{editor.type ?editor.type.desc : '请在上方选择一个帖子类型'}}</span>
            </div>
            <div style="margin-top: 15px;height: 410px; overflow: hidden;border-radius: 5px"
                 v-loading="editor.uploading" element-loading-text="图片上传中......">
                <quill-editor v-model:content="editor.text" style="height: calc(100% - 45px);"
                              content-type="delta" ref="refEditor" placeholder="输入内容" :options="editorOption"/>
            </div>
            <div style="display: flex;justify-content: space-between;margin-top: 5px">
                <div style="color: #131313;font-size: 13px">
                    当前字数 {{contentLength}} (最大支持5000字)
                </div>
                <div>
                    <el-button type="success" :icon="Position" @click="submitTopic" plain>{{submitButton}}</el-button>
                </div>
            </div>

        </el-drawer>
    </div>
</template>

<style scoped>
:deep(.el-drawer) {
    width: 800px;
    margin: auto;
    border-radius: 10px 10px 0 0;
}
:deep(.el-drawer__header){
    margin: 0;
}
</style>