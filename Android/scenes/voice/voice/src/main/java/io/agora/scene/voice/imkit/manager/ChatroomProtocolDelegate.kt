package io.agora.scene.voice.imkit.manager

import android.text.TextUtils
import io.agora.CallBack
import io.agora.ValueCallBack
import io.agora.chat.ChatClient
import io.agora.chat.ChatRoomManager
import io.agora.scene.voice.annotation.MicClickAction
import io.agora.scene.voice.annotation.MicStatus
import io.agora.scene.voice.imkit.bean.ChatMessageData
import io.agora.scene.voice.imkit.custorm.CustomMsgHelper
import io.agora.scene.voice.imkit.custorm.CustomMsgType
import io.agora.scene.voice.imkit.custorm.OnMsgCallBack
import io.agora.scene.voice.service.VoiceBuddyFactory
import io.agora.scene.voice.service.VoiceMemberModel
import io.agora.scene.voice.service.VoiceMicInfoModel
import io.agora.scene.voice.service.VoiceRoomApply
import io.agora.voice.buddy.config.ConfigConstants
import io.agora.voice.buddy.tool.GsonTools
import io.agora.voice.buddy.tool.LogTools.logE

class ChatroomProtocolDelegate constructor(
    private val roomId: String
) {
    companion object {
        private const val TAG = "ChatroomProtocolDelegate"
    }

    private var roomManager: ChatRoomManager = ChatClient.getInstance().chatroomManager()
    lateinit var ownerBean: VoiceMemberModel

    /////////////////////// mic ///////////////////////////

    /**
     * 初始化麦位信息
     */
    fun initMicInfo(roomType: Int, ownerBean: VoiceMemberModel, callBack: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        this@ChatroomProtocolDelegate.ownerBean = ownerBean
        if (roomType == ConfigConstants.RoomType.Common_Chatroom) {
            attributeMap["use_robot"] = "0"
            attributeMap["robot_volume"] = "50"
            attributeMap["mic_0"] = GsonTools.beanToString(VoiceMicInfoModel(0, ownerBean, MicStatus.Normal)).toString()
            for (i in 1..7) {
                var key = "mic_$i"
                var status = MicStatus.Idle
                if (i >= 6) status = MicStatus.BotInactive
                var mBean = GsonTools.beanToString(VoiceMicInfoModel(i, null, status))
                if (mBean != null) {
                    attributeMap[key] = mBean
                }
            }
        } else if (roomType == ConfigConstants.RoomType.Spatial_Chatroom) {

        }
        roomManager.asyncSetChatroomAttributesForced(
            roomId, attributeMap, true
        ) { code, result_map ->
            if (code == 200) {
                callBack.onSuccess()
                "update result onSuccess: ".logE(TAG)
            } else {
                callBack.onError(code, result_map.toString())
                "update result onError: $code $result_map ".logE(TAG)
            }
        }
    }

    /**
     * 从服务端获取所有麦位信息
     */
    fun getMicInfoFromServer(): MutableMap<String, VoiceMicInfoModel> {
        var micInfoMap = mutableMapOf<String, VoiceMicInfoModel>()
        val keyList: MutableList<String> = java.util.ArrayList()
        for (i in 0..7) {
            keyList[i] = "mic_$i"
        }
        roomManager.asyncFetchChatroomAttributesFromServer(roomId,keyList,object :
            ValueCallBack<MutableMap<String, String>> {
            override fun onSuccess(value: MutableMap<String, String>?) {
                value?.let {
                    ChatroomCacheManager.cacheManager.clearMicInfo()
                    ChatroomCacheManager.cacheManager.setMicInfo(it)
                    for (entry in value.entries) {
                        var bean = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)
                        if (bean != null) {
                            micInfoMap[entry.key] = bean
                        }
                    }
                }
            }

            override fun onError(error: Int, desc: String?) {
                "onError: $error $desc".logE("asyncFetchChatRoomAllAttributesFromServer")
            }

        })
        return micInfoMap
    }

    /**
     * 从本地缓存获取所有麦位信息
     */
    fun getMicInfo(): MutableMap<String, VoiceMicInfoModel> {
        val micInfoMap = mutableMapOf<String, VoiceMicInfoModel>()
        var localMap = ChatroomCacheManager.cacheManager.getMicInfoMap()
        if (localMap != null) {
            for (entry in localMap.entries) {
                var bean = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)
                if (bean != null) {
                    micInfoMap[entry.key] = bean
                }
            }
        }
        return micInfoMap
    }

    /**
     * 从本地获取指定麦位信息
     */
    private fun getMicInfo(micIndex: Int): VoiceMicInfoModel? {
        return ChatroomCacheManager.cacheManager.getMicInfoByIndex(micIndex)
    }

    /**
     * 从服务端获取指定麦位信息
     */
    fun getMicInfoByIndexFromServer(micIndex: Int): VoiceMicInfoModel {
        val keyList: MutableList<String> = java.util.ArrayList()
        var micBean = VoiceMicInfoModel(-99, null, -99)
        keyList.add(getMicIndex(micIndex))
        roomManager.asyncFetchChatroomAttributesFromServer(roomId, keyList, object :
            ValueCallBack<MutableMap<String, String>> {
            override fun onSuccess(value: MutableMap<String, String>?) {
                for (entry in value?.entries!!) {
                    micBean = GsonTools.toBean(entry.value, VoiceMicInfoModel::class.java)!!
                    "getMicInfoByIndex onSuccess: ".logE(TAG)
                }
            }

            override fun onError(error: Int, desc: String?) {
                "getMicInfoByIndex onError: $error $desc".logE(TAG)
            }
        })
        return micBean
    }

    /**
     * 下麦
     */
    fun leaveMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.OffStage, false, callback)
    }

    /**
     * 交换麦位
     */
    fun changeMic(fromMicIndex: Int, toMicIndex: Int, callback: ValueCallBack<Map<Int, VoiceMicInfoModel>>) {
        val attributeMap = ChatroomCacheManager.cacheManager.getMicInfoMap()
        val fromKey = getMicIndex(fromMicIndex)
        val toKey = getMicIndex(toMicIndex)
        val fromBean = getMicInfo(fromMicIndex)
        val toMicBean = getMicInfo(toMicIndex)
        val fromBeanValue = GsonTools.beanToString(fromBean)
        val toBeanValue = GsonTools.beanToString(toMicBean)
        if (toMicBean != null && fromBean != null && toMicBean.micStatus == -1) {
            if (toBeanValue != null) {
                attributeMap?.put(fromKey, toBeanValue)
            }
            if (fromBeanValue != null) {
                attributeMap?.put(toKey, fromBeanValue)
            }
            roomManager.asyncSetChatroomAttributes(
                roomId, attributeMap, true
            ) { code, result_map ->
                if (code == 200) {
                    var map = mutableMapOf<Int, VoiceMicInfoModel>()
                    map[fromMicIndex] = toMicBean
                    map[toMicIndex] = fromBean
                    callback.onSuccess(map)
                    "update result onSuccess: ".logE(TAG)
                } else {
                    callback.onError(code, result_map.toString())
                    "update result onError: $code $result_map ".logE(TAG)
                }
            }
        }
    }

    /**
     * 关麦
     */
    fun muteLocal(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.Mute, false, callback)
    }

    /**
     * 取消关麦
     */
    fun unMuteLocal(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.UnMute, false, callback)
    }

    /**
     * 禁言指定麦位
     */
    fun forbidMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.ForbidMic, true, callback)
    }

    /**
     * 取消指定麦位禁言
     */
    fun unForbidMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.UnForbidMic, true, callback)
    }

    /**
     * 踢用户下麦
     */
    fun kickOff(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.KickOff, true, callback)
    }

    /**
     * 锁麦
     */
    fun lockMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.Lock, true, callback)
    }

    /**
     * 取消锁麦
     */
    fun unLockMic(micIndex: Int, callback: ValueCallBack<VoiceMicInfoModel>) {
        updateMicByResult(micIndex, MicClickAction.UnLock, true, callback)
    }

    /**
     * 获取上麦申请列表
     */
    fun fetchRaisedList(): MutableList<VoiceMemberModel> {
        return ChatroomCacheManager.cacheManager.getSubmitMicList()
    }

    /**
     * 申请上麦
     */
    fun startMicSeatApply(micIndex: Int? = null, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        var voiceRoomApply = VoiceRoomApply()
        var memberBean = VoiceMemberModel()
        memberBean.chatUid = VoiceBuddyFactory.get().getVoiceBuddy().chatUid()
        memberBean.rtcUid = VoiceBuddyFactory.get().getVoiceBuddy().rtcUid()
        memberBean.nickName = VoiceBuddyFactory.get().getVoiceBuddy().nickName()
        memberBean.portrait = VoiceBuddyFactory.get().getVoiceBuddy().headUrl()
        voiceRoomApply.member = memberBean
        voiceRoomApply.created_at = System.currentTimeMillis()
        if (micIndex != null) {
            voiceRoomApply.index = micIndex
        }
        attributeMap["user"] = GsonTools.beanToString(voiceRoomApply).toString()
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_APPLY_SITE, attributeMap, callback)
    }

    /**
     * 同意上麦申请
     */
    fun acceptMicSeatApply(micIndex: Int? = null, callback: ValueCallBack<VoiceMicInfoModel>) {
        if (micIndex != null) {
            updateMicByResult(micIndex, MicClickAction.Accept, false, callback)
        } else {
            updateMicByResult(getFirstFreeMic(), MicClickAction.Accept, false, callback)
        }
    }

    /**
     * 拒绝上麦申请
     */
    fun rejectSubmitMic() {
        // TODO: 本期暂无 拒绝上麦申请
    }

    /**
     * 撤销上麦申请
     */
    fun cancelSubmitMic(chatUid: String, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        var userBeam = VoiceMemberModel(chatUid)
        attributeMap["user"] = GsonTools.beanToString(userBeam).toString()
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_APPLY_SITE, attributeMap, callback)
    }

    /**
     * 邀请上麦列表
     */
    fun fetchRoomMembers():MutableList<VoiceMemberModel> {
        return ChatroomCacheManager.cacheManager.getMemberList()
    }

    /**
     * 邀请上麦
     */
    fun invitationMic(chatUid: String, micIndex: Int? = null, callback: CallBack) {
        val attributeMap = mutableMapOf<String, String>()
        var userBeam = VoiceMemberModel(chatUid)
        attributeMap["user"] = GsonTools.beanToString(userBeam).toString()
        sendChatroomEvent(true, chatUid, CustomMsgType.CHATROOM_INVITE_SITE, attributeMap, callback)
    }

    /**
     * 用户拒绝上麦邀请
     */
    fun refuseInviteToMic(chatUid: String, callback: CallBack) {
        // TODO:  ios 没实现 需要确认是否需要实现
        val attributeMap = mutableMapOf<String, String>()
        var userBeam = VoiceMemberModel(chatUid)
        attributeMap["user"] = GsonTools.beanToString(userBeam).toString()
        sendChatroomEvent(true, ownerBean.chatUid, CustomMsgType.CHATROOM_INVITE_REFUSED_SITE, attributeMap, callback)
    }

    /**
     * 用户同意上麦邀请
     */
    fun acceptMicSeatInvitation(micIndex: Int? = null, callback: ValueCallBack<VoiceMicInfoModel>) {
        if (micIndex != null) {
            updateMicByResult(micIndex, MicClickAction.Accept, false, callback)
        } else {
            updateMicByResult(getFirstFreeMic(), MicClickAction.Accept, false, callback)
        }
    }

    /////////////////////////// room ///////////////////////////////

    /**
     * 更新公告
     */
    fun updateAnnouncement(content: String, callback: CallBack) {
        roomManager.asyncUpdateChatRoomAnnouncement(roomId, content, object : CallBack {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code, error)
            }
        })
    }

    /**
     * 是否启用机器人
     * @param enable true 启动机器人，false 关闭机器人
     */
    fun enableRobot(enable: Boolean, callback: ValueCallBack<Map<Int, VoiceMicInfoModel>>) {
        val attributeMap = mutableMapOf<String, String>()
        val currentUser = VoiceBuddyFactory.get().getVoiceBuddy().chatUid()
        var robot6 = VoiceMicInfoModel()
        var robot7 = VoiceMicInfoModel()
        var isEnable: String
        if (TextUtils.equals(ownerBean.chatUid, currentUser)) {
            if (enable) {
                robot6.micIndex = 6
                robot6.micStatus = MicStatus.BotActivated
                robot7.micIndex = 7
                robot7.micStatus = MicStatus.BotActivated
                isEnable = "1"
            } else {
                robot6.micIndex = 6
                robot6.micStatus = MicStatus.BotInactive
                robot7.micIndex = 7
                robot7.micStatus = MicStatus.BotInactive
                isEnable = "0"
            }
            attributeMap["mic_6"] = GsonTools.beanToString(robot6).toString()
            attributeMap["mic_7"] = GsonTools.beanToString(robot7).toString()
            attributeMap["use_robot"] = isEnable
            roomManager.asyncSetChatroomAttributes(
                roomId, attributeMap, true
            ) { code, result ->
                if (code == 200 && result.isEmpty()) {
                    var map = mutableMapOf<Int, VoiceMicInfoModel>()
                    map[6] = robot6
                    map[7] = robot7
                    callback.onSuccess(map)
                    "update result onSuccess: ".logE(TAG)
                } else {
                    callback.onError(code, result.toString())
                    "update result onError: $code $result ".logE(TAG)
                }
            }
        }
    }

    /**
     * 更新机器人音量
     * @param value 音量
     */
    fun updateRobotVolume(value: Int, callback: CallBack) {
        roomManager.asyncSetChatroomAttribute(roomId, "robot_volume", value.toString(), true, object :
            CallBack {
            override fun onSuccess() {
                callback.onSuccess()
            }

            override fun onError(code: Int, error: String?) {
                callback.onError(code, error)
            }
        })
    }

    /**
     * 更新指定麦位信息并返回更新成功的麦位信息
     * 0:正常状态 1:闭麦 2:禁言 3:锁麦 4:锁麦和禁言 -1:空闲 5:机器人专属激活状态 -2:机器人专属关闭状态
     */
    private fun updateMicByResult(
        micIndex: Int, @MicClickAction clickAction: Int, isForced: Boolean, callback: ValueCallBack<VoiceMicInfoModel>
    ) {
        val voiceMicInfo = getMicInfo(micIndex) ?: return
        updateMicStatusByAction(voiceMicInfo, clickAction)
        voiceMicInfo.micIndex = micIndex
        val value = GsonTools.beanToString(voiceMicInfo) ?: return
        if (isForced) {
            roomManager.asyncSetChatroomAttributeForced(roomId, getMicIndex(micIndex),
                value, true, object : CallBack {
                    override fun onSuccess() {
                        callback.onSuccess(voiceMicInfo)
                        "Forced updateMic onSuccess: ".logE(TAG)
                    }

                    override fun onError(code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "Forced updateMic onError: $code $desc".logE(TAG)
                    }
                })
        } else {
            roomManager.asyncSetChatroomAttribute(roomId, getMicIndex(micIndex),
                value, true, object : CallBack {
                    override fun onSuccess() {
                        callback.onSuccess(voiceMicInfo)
                        "updateMic onSuccess: ".logE(TAG)
                    }

                    override fun onError(code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "updateMic onError: $code $desc".logE(TAG)
                    }
                })
        }
    }

    /**
     * 根据麦位原状态与action 更新麦位状态
     */
    private fun updateMicStatusByAction(micInfo: VoiceMicInfoModel, @MicClickAction action: Int) {
        when (action) {
            MicClickAction.ForbidMic -> {
                // 禁言（房主操作）
                if (micInfo.micStatus == MicStatus.Lock) {
                    micInfo.micStatus = MicStatus.LockForceMute
                } else {
                    micInfo.micStatus = MicStatus.ForceMute
                }
            }
            // 取消禁言（房主操作）
            MicClickAction.UnForbidMic -> {
                if (micInfo.micStatus == MicStatus.LockForceMute) {
                    micInfo.micStatus = MicStatus.Lock
                } else {
                    if (micInfo.member == null) {
                        micInfo.micStatus = MicStatus.Idle
                    } else {
                        micInfo.micStatus = MicStatus.Normal
                    }
                }
            }
            // 关麦（麦位用户操作包括房主操作自己）
            MicClickAction.Mute -> {
                micInfo.micStatus = MicStatus.Mute
            }
            // 关闭座位（房主操作）
            MicClickAction.UnMute -> {
                if (micInfo.member == null) {
                    micInfo.micStatus = MicStatus.Idle
                } else {
                    micInfo.micStatus = MicStatus.Normal
                }
            }
            // 关闭座位（房主操作）
            MicClickAction.Lock -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.LockForceMute
                } else {
                    micInfo.micStatus = MicStatus.Lock
                }
            }
            // 打开座位（房主操作）
            MicClickAction.UnLock -> {
                if (micInfo.micStatus == MicStatus.LockForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    if (micInfo.member == null) {
                        micInfo.micStatus = MicStatus.Idle
                    } else {
                        micInfo.micStatus = MicStatus.Normal
                    }
                }
            }
            // 强制下麦（房主操作）
            MicClickAction.KickOff -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    micInfo.micStatus = MicStatus.Idle
                }
            }
            // 下麦（嘉宾操作）
            MicClickAction.OffStage -> {
                if (micInfo.micStatus == MicStatus.ForceMute) {
                    micInfo.micStatus = MicStatus.ForceMute
                } else {
                    micInfo.micStatus = MicStatus.Idle
                }
            }
            // 接受邀请/接受申请
            MicClickAction.Accept -> {
                micInfo.micStatus = MicStatus.Normal
            }
            MicClickAction.Invite -> {
                // nothing
            }
        }
    }

    private fun sendChatroomEvent(
        isSingle: Boolean, chatUid: String?, eventType: CustomMsgType,
        params: MutableMap<String, String>, callback: CallBack
    ) {
        if (isSingle) {
            CustomMsgHelper.getInstance().sendCustomSingleMsg(chatUid,
                eventType.getName(), params, object : OnMsgCallBack() {
                    override fun onSuccess(message: ChatMessageData?) {
                        callback.onSuccess()
                        "sendCustomSingleMsg onSuccess: $message".logE(TAG)
                    }

                    override fun onError(messageId: String?, code: Int, desc: String?) {
                        callback.onError(code, desc)
                        "sendCustomSingleMsg onError: $code $desc".logE(TAG)
                    }
                })
        } else {
            CustomMsgHelper.getInstance().sendCustomMsg(roomId, params, object : OnMsgCallBack() {
                override fun onSuccess(message: ChatMessageData?) {
                    callback.onSuccess()
                    "sendCustomMsg onSuccess: $message".logE(TAG)
                }

                override fun onError(messageId: String?, code: Int, desc: String?) {
                    super.onError(messageId, code, desc)
                    callback.onError(code, desc)
                    "sendCustomMsg onError: $code $desc".logE(TAG)
                }
            })
        }

    }

    /**
     *  按麦位顺序查询空麦位
     */
    private fun getFirstFreeMic(): Int {
        var indexList: MutableList<Int> = mutableListOf<Int>()
        var micInfo = ChatroomCacheManager.cacheManager.getMicInfoMap()
        if (micInfo != null) {
            for (mutableEntry in micInfo) {
                var bean = GsonTools.toBean(mutableEntry.value, VoiceMicInfoModel::class.java)
                if (bean != null && bean.micStatus == -1) {
                    indexList.add(bean.micIndex)
                }
            }
        }
        indexList.sortBy { it }
        return indexList[indexList.lastIndex]
    }

    private fun getMicIndex(index: Int): String {
        return "mic_$index"
    }
}