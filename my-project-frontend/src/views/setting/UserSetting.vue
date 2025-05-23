<script setup>

import Card from "@/components/Card.vue";
import {Message, Notebook, Refresh, Select, User} from "@element-plus/icons-vue";
import {useStore} from "@/store";
import {computed, reactive, ref} from "vue";
import {accessHeader, get, post} from "@/net";
import {ElMessage} from "element-plus";
import axios from "axios";

const store = useStore();
const desc = ref('')
const registerTime = computed(() => new Date(store.user.registerTime).toLocaleString())
const baseFormRef = ref()
const emailFormRef = ref()
const baseForm = reactive({
    username: '',
    gender: '',
    phone: '',
    qq: '',
    wx: '',
    desc: '',

})
const emailForm = reactive({
    email: '',
    code: ''
})
const validateUsername = (rule, value, callback) => {
    if (value == '') {
        callback(new Error('请输入用户名'))
    } else if (!/^[a-zA-Z0-9\u4e00-\u9fa5]+$/.test(value)) {
        callback(new Error('用户名不能包含特殊字符'))
    } else
        callback()
}
const rules = {
    username: [
        {validator: validateUsername, trigger: ['blur', 'change']},
        {min: 2, max: 10, message: '用户名的长度必须在2-10个字符', trigger: ['blur', 'change']},
    ],
    email: [
        {required: true, message: '请输入邮件地址', trigger: ['blur','change']},
        {type: 'email', message: '请输入合法的电子邮件地址', trigger: ['blur', 'change']},
    ],
    code: [
        {required: true, message: '请输入获取的验证码', trigger: 'blur'},
    ],
    phone: [
        {
            pattern: /^1[3-9]\d{9}$/,
            message: '请输入有效的中国大陆手机号',
            trigger: ['blur', 'change']
        },
        // { message: '请输入手机号', trigger: ['blur','change'] },

    ],
    qq: [
        {
            pattern: /^[0-9]/,
            message: '请输入有效的QQ号',
            trigger: ['blur', 'change']
        },
    ]
}
const loading = reactive({
    form: true,
    base: false
})

function saveDetails() {
    baseFormRef.value.validate(isValid => {
        if (isValid) {
            loading.base = true
            post('/api/user/save-details', baseForm, () => {
                ElMessage.success('用户信息保存成功')
                store.user.username = baseForm.username
                desc.value = baseForm.desc
                loading.base = false
            }, (message) => {
                ElMessage.warning(message)
                loading.base = false
            })
        }
    })
}

get('api/user/details', data => {
    baseForm.username = store.user.username
    baseForm.gender = data.gender
    baseForm.qq = data.qq
    baseForm.wx = data.wx
    baseForm.phone = data.phone
    baseForm.desc = desc.value = data.desc
    emailForm.email = store.user.email
    loading.form = false
})
const coldTime = ref(0)
const isEmailValid = ref(true)
const onValidate =(prop, isValid) =>{
    // console.log(prop)
    if (prop === 'email')
        isEmailValid.value = isValid
}

function sendEmailCode() {
   emailFormRef.value.validate(isValid =>{
       if (isValid){
           coldTime.value = 60
           get(`api/auth/ask-code?email=${emailForm.email}&type=modify`, () => {
               ElMessage.success(`验证码已成功发送到：${emailForm.email}`)
               const handle = setInterval(() => {
                   coldTime.value--
                   if (coldTime.value === 0) {
                       clearInterval(handle)
                   }
               }, 1000)
           }, (message) => {
               ElMessage.warning(message)
               coldTime.value = 0
           })
       }
   })

}
function modifyEmail() {
    emailFormRef.value.validate(isValid =>{
        if(isValid){
            post('/api/user/modify-email',emailForm,()=>{
                ElMessage.success('邮件修改成功')
                store.user.email = emailForm.email
                emailForm.code = ''
            })
        }
    })
}
// console.log(coldTime.value)
// console.log(isEmailValid)
function beforeAvatarUpload(rawFile) {
    if (rawFile.type !=='image/jpeg' && rawFile.type !=='image/png'){
        ElMessage.error("头像只能是 JPG/PNG 格式的")
        return false
    }else  if (rawFile.size/1024 > 100){
        ElMessage.error("头像大小不能超过100KB")
        return  false
    }
    return  true
}
function uploadSuccess(response){
    ElMessage.success('头像上传成功')
    store.user.avatar=response.data
}
// console.log(axios.defaults.baseURL+'api/image/avatar'); // 打印出uploadUrl以检查它的值


</script>

<template>
    <div style="display: flex">
        <div class="settings-left">
            <card v-loading="loading.form" :icon="User" desc="在这里编辑个人信息" title="账号信息设置">
                <el-form ref="baseFormRef" :model="baseForm" :rules="rules" label-position="top" style="margin: 0 10px 10px 10px"
                         @validate="onValidate">
                    <el-form-item label="用户名" prop="username">
                        <el-input v-model="baseForm.username" maxlength="10"></el-input>
                    </el-form-item>
                    <el-form-item label="性别">
                        <el-radio-group v-model="baseForm.gender">
                            <el-radio :label="0">男</el-radio>
                            <el-radio :label="1">女</el-radio>
                        </el-radio-group>
                    </el-form-item>
                    <el-form-item label="手机号" prop="phone">
                        <el-input v-model="baseForm.phone" maxlength="11"></el-input>
                    </el-form-item>
                    <el-form-item label="QQ号" prop="qq">
                        <el-input v-model="baseForm.qq" maxlength="11"></el-input>
                    </el-form-item>
                    <el-form-item label="微信号" prop="wx">
                        <el-input v-model="baseForm.wx" maxlength="20"></el-input>
                    </el-form-item>
                    <el-form-item label="个人简介" prop="desc">
                        <el-input v-model="baseForm.desc" :rows="5" maxlength="300" type="textarea"></el-input>
                    </el-form-item>
                    <el-form-item>
                        <div>
                            <el-button :icon="Select" :loading="loading.base" type="success" @click="saveDetails">
                                保存信息
                            </el-button>
                        </div>
                    </el-form-item>
                </el-form>
            </card>
            <card :icon="Message" desc="请设置你的电子邮件" style="margin-top: 10px" title="电子邮件设置">
                <el-form ref="emailFormRef" @validate="onValidate" :model="emailForm" :rules="rules" label-position="top"
                         style="margin: 0 10px 10px 10px">
                    <el-form-item label="电子邮件" prop="email">
                        <el-input v-model="emailForm.email"></el-input>
                    </el-form-item>
                    <el-form-item prop="code">
                        <el-row :gutter="10" style="width: 100% ">
                            <el-col :span="10">
                                <el-input v-model="emailForm.code" placeholder="请获取验证码"/>
                            </el-col>
                            <el-col :span="6">
                                <el-button :disabled="!isEmailValid || coldTime > 0"
                                           plain style="width: 100%" type="success" @click="sendEmailCode">
                                    {{ coldTime > 0 ? `请等待${coldTime}秒后再获取` : '获取验证码' }}

                                </el-button>
                            </el-col>
                        </el-row>
                    </el-form-item>
                    <div>
                        <el-button :icon="Refresh" @click="modifyEmail" type="success">更新电子邮件</el-button>
                    </div>
                </el-form>
            </card>
        </div>
        <div class="settings-right">
            <div style="position: sticky ;top:20px">
                <card>
                    <div style="text-align: center ;padding: 5px 15px 0 15px">
                        <el-avatar :size="70" :src="store.avatarUrl"/>
                        <div style="margin: 5px 0">
                           <el-upload :action="axios.defaults.baseURL+'/api/image/avatar'"
                                      :show-file-list="false"
                                      :before-upload="beforeAvatarUpload"
                                      :on-success="uploadSuccess"
                                      :headers="accessHeader()"
                           >
                            <el-button size="small" round>修改头像</el-button>
                            </el-upload>
                        </div>
                        <div style="font-weight: bold">你好,{{ store.user.username }}</div>
                    </div>
                    <el-divider style="margin: 10px 0"/>
                    <div style="font-size: 14px ;color: gray;padding: 10px">
                        {{ desc || '这个用户很懒，没有填个人简介' }}
                    </div>
                </card>
                <card style="margin-top: 10px ;font-size: 15px">
                    <div>第一次的到来:{{ registerTime }}</div>
                    <div style="color: gray">欢迎加入我们的交流论坛</div>
                </card>
            </div>

        </div>
    </div>
</template>

<style scoped>
.settings-left {
    flex: 1;
    margin: 20px;
}

.settings-right {
    width: 300px;
    margin: 20px;
}
</style>