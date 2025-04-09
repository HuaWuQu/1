import {defineStore} from "pinia";
import axios from "axios";
export const  useStore =defineStore('genral',{
    state:() =>{
        return {
            user:{
                id: -1,
                username:'',
                email:'',
                role:'',
                avatar:null,
                registerTime:null
            },
            forum: {
                types:[]
            }
        }
    },getters:{    /* 在后端去获取头像*/
        avatarUrl() {
            if (this.user.avatar)return `${axios.defaults.baseURL}/images${this.user.avatar}`
            else return ''
        }
    },actions:{
        findTypeById(id){
            for (let type of this.forum.types) {
                if (type.id === id)
                    return type
            }
        }
    }
})