<script setup>
import {get, logout} from '@/net'
import router from "@/router";
import {useStore} from "@/store";
import {reactive, ref} from "vue";
import {Back, Location, Message, Operation, User} from "@element-plus/icons-vue";

const store = useStore()
const loding = ref(true)
// const input = ref('搜索想搜')
const serchInput = reactive({
    type: '1',
    text: ''
})

get('api/user/info', (data) => {
    store.user = data
    loding.value = false
})

function userLogout() {
    logout(() => router.push("/"))
}
</script>


<template>
    <div v-loading="loding" class="main-content" element-loading-text="正在进入系统。。。。。。">
        <el-container v-if="!loding" style="height: 100%">
            <el-header class="main-content-header">
                <el-image class="logo" src="index.png"></el-image>
                <div style="flex: 1;padding: 0 20px; text-align: center">
                    <el-input v-model="serchInput.text" placeholder="搜索想搜" style="width: 100% ;max-width: 500px;">
                        <template #prefix>
                            <el-icon>
                                <Search/>
                            </el-icon>
                        </template>
                        <template #append>
                            <el-select v-model="serchInput.type" style="width: 100px;font-size: small">
                                <el-option label="帖子广场" value="1"/>
                                <el-option label="失物招领" value="2"/>
                                <el-option label="表白墙" value="3"/>
                            </el-select>
                        </template>
                    </el-input>
                </div>
                <div class="user-info" style="flex: 1">
                    <div class="profile">
                        <div>{{ store.user.username }}</div>
                        <div>{{ store.user.email }}</div>
                    </div>
                    <el-dropdown>
                        <el-avatar
                                :src="store.avatarUrl"></el-avatar>
                        <template #dropdown>
                            <el-dropdown-item>
                                <el-icon>
                                    <Operation/>
                                </el-icon>
                                个人设置
                            </el-dropdown-item>
                            <el-dropdown-item>
                                <el-icon>
                                    <Message/>
                                </el-icon>
                                消息列表
                            </el-dropdown-item>
                            <el-dropdown-item divided @click="userLogout">
                                <el-icon>
                                    <Back/>
                                </el-icon>
                                退出登录
                            </el-dropdown-item>
                        </template>

                    </el-dropdown>

                </div>
            </el-header>
            <el-container>
                <el-aside width="230px">
                    <el-scrollbar style="height: calc(100vh - 55px)">
                        <el-menu :default-active="$route.path" default-active="1-1"
                                 router style="min-height: calc(100vh - 55px)">
                            <el-sub-menu index="1">
                                <template #title>
                                    <el-icon>
                                        <Memo/>
                                    </el-icon>
                                    <span><b>校园论坛</b></span>
                                </template>
                                <el-menu-item index="/index">
                                    <el-icon>
                                        <ChatLineSquare/>
                                    </el-icon>
                                    <template #title>帖子广场</template>
                                </el-menu-item>
                                <el-menu-item>
                                    <el-icon>
                                        <Promotion/>
                                    </el-icon>
                                    <template #title>失物招领</template>
                                </el-menu-item>
                                <el-menu-item>
                                    <el-icon>
                                        <WindPower/>
                                    </el-icon>
                                    <template #title>校园活动</template>
                                </el-menu-item>
                                <el-menu-item>
                                    <el-icon>
                                        <GobletFull/>
                                    </el-icon>
                                    <template #title>表白墙</template>
                                </el-menu-item>
                                <el-menu-item>
                                    <el-icon>
                                        <Reading/>
                                    </el-icon>
                                    <template #title>海文考研
                                        <el-tag size="small" style="margin-left: 10px">合作机构</el-tag>
                                    </template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="2">
                                <template #title>
                                    <el-icon>
                                        <View/>
                                    </el-icon>
                                    <span><b>发现</b></span>
                                </template>
                                <el-menu-item>
                                    <el-icon>
                                        <Tickets/>
                                    </el-icon>
                                    <template #title>成绩查询</template>
                                </el-menu-item>
                                <el-menu-item>
                                    <el-icon>
                                        <DataBoard/>
                                    </el-icon>
                                    <template #title>课程表</template>
                                </el-menu-item>
                            </el-sub-menu>
                            <el-sub-menu index="3">
                                <template #title>
                                    <el-icon>
                                        <Operation/>
                                    </el-icon>
                                    <span><b>个人设置</b></span>
                                </template>
                                <el-menu-item index="/index/user-setting">
                                    <el-icon>
                                        <User/>
                                    </el-icon>
                                    <template #title>信息设置</template>
                                </el-menu-item>
                                <el-menu-item index="/index/privacy-setting">
                                    <el-icon>
                                        <Lock/>
                                    </el-icon>
                                    <template #title>账号安全设置</template>
                                </el-menu-item>
                            </el-sub-menu>
                        </el-menu>
                    </el-scrollbar>
                </el-aside>
                <el-main class="main-content-page">
                    <el-scrollbar style="height: calc(100vh - 55px)">
                        <router-view v-slot="{Component}">
                            <transition mode="out-in" name="el-fade-in-linear">
                                <component :is="Component" style="height: 100%"/>
                            </transition>
                        </router-view>
                    </el-scrollbar>
                </el-main>
            </el-container>
        </el-container>
    </div>
</template>


<style lang="less" scoped>
.main-content {
  height: 100vh;
  width: 100vw;
}

.main-content-header {
  border-bottom: solid 1px var(--el-border-color);
  height: 55px;
  display: flex;
  align-items: center;
  box-sizing: border-box;

  .logo {
    height: 45px;
    width: 200px;
  }

  .user-info {
    display: flex;
    justify-content: flex-end;
    align-items: center;

    .el-avatar:hover {
      cursor: pointer;
    }

    .profile {
      text-align: right;
      margin-right: 20px;

      :first-child {
        font-size: 18px;
        font-weight: bold;
        line-height: 20px;
      }

      :last-child {
        font-size: 10px;
        color: gray;
      }
    }
  }
}

.main-content-page {
  padding: 0;
  background-color: #f7f8fa;
}

.dark .main-content-page {
  background-color: #1a1a1c;
}
</style>
