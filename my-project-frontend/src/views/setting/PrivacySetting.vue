<script setup>

import Card from "@/components/Card.vue";
import {Setting, Switch, Lock} from "@element-plus/icons-vue";
import {reactive, ref} from "vue";
import {get, post} from "@/net";
import {ElMessage} from "element-plus";

const form =reactive( {
    password:'',
    new_password: '',
    new_password_repeat:''
})
const validatePassword = (rule, value, callback) => {
    if (value === '') {
        callback(new Error('请再次输入密码'))
    } else if (value !== form.new_password) {
        callback(new Error("两次输入的密码不一致"))
    } else {
        callback()
    }
}
const rules = {
    password:[
        {required : true ,message:'输入原来的密码', trigger: 'blur'}
    ],
    new_password:[
        {required : true ,message:'输入新的密码', trigger: 'blur'},
        {min:6 ,max:20 ,message:'密码长度在6-20字符之间',trigger: ['blur']}
    ],
    new_password_repeat:[
        {required : true ,message:'输入新的密码', trigger: 'blur'},
        {validator: validatePassword,trigger: ['blur','change']}
    ]
}
const formRef = ref()
const valid = ref(false)
const onValidate =(prop, isValid) => valid.value = isValid

function resetPassword(){
    formRef.value.validate(valid => {
        if (valid){
            post('api/user/change-password',form, ()=>{
                ElMessage.success('密码修改成功')
                formRef.value.resetFields();
            })
        }
    })
}
const saving = ref(true)
const  privacy = reactive({
    phone:false,
    wx:false,
    qq:false,
    email:false,
    gender:false
})
get("/api/user/privacy",data =>{
    privacy.email = data.email;
    privacy.wx =data.wx;
    privacy.qq =data.qq;
    privacy.phone= data.phone;
    privacy.gender =data.gender;
    saving.value =false;
})

function  savePrivacy(type,status){
    saving.value =true
    post('api/user/save-privacy',{
        type:type,
        status :status
    },()=>{
        ElMessage.success('隐私设置修改成功')
        saving.value=false;
    })
}

</script>

<template>
    <div>
        <div style="margin-top: 20px">
            <card :icon="Setting" title="隐私设置" desc="设置公开信息" v-loading="saving">
                <div class="checkbox-list">
                    <el-checkbox @change="savePrivacy('phone',privacy.phone)" v-model="privacy.phone">公开手机号</el-checkbox>
                    <el-checkbox @change="savePrivacy('email',privacy.email)" v-model="privacy.email">公开电子邮件地址</el-checkbox>
                    <el-checkbox @change="savePrivacy('wx',privacy.wx)" v-model="privacy.wx">公开微信号</el-checkbox>
                    <el-checkbox @change="savePrivacy('qq',privacy.qq)" v-model="privacy.qq">公开QQ号</el-checkbox>
                    <el-checkbox @change="savePrivacy('gender',privacy.gender)" v-model="privacy.gender">公开性别</el-checkbox>
                </div>
            </card>
            <card style="margin: 20px 0" :icon="Setting" title="修改密码" desc="在这里修改密码">
                  <el-form :rules="rules" :model="form" ref="formRef" @validate="onValidate" label-width="100" style=" margin: 20px">
                    <el-form-item label="当前密码" prop="password">
                        <el-input :prefix-icon="Lock" placeholder="当前密码" v-model="form.password"  maxlength="20" />
                    </el-form-item>
                    <el-form-item label="新的密码" prop="new_password">
                        <el-input :prefix-icon="Lock" placeholder="新的密码" v-model="form.new_password"   maxlength="20" />
                    </el-form-item>
                    <el-form-item label="确认密码" prop="new_password_repeat">
                        <el-input :prefix-icon="Lock" placeholder="确认密码" v-model="form.new_password_repeat"   maxlength="20" />
                    </el-form-item>
                    <div style="text-align: center">
                        <el-button :icon="Switch" @click="resetPassword" type="success">重置密码</el-button>
                    </div>
                </el-form>
            </card>
        </div>
    </div>
</template>

<style scoped>
    .checkbox-list{
        margin: 10px 0 0 10px;
        display: flex;
        flex-direction: column;
    }
</style>