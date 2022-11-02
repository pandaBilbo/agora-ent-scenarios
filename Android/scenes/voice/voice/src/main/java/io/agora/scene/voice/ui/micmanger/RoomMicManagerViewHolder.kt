package io.agora.scene.voice.ui.micmanger

import io.agora.scene.voice.R
import io.agora.scene.voice.bean.MicManagerBean
import io.agora.scene.voice.databinding.VoiceItemRoomMicManagerBinding
import io.agora.voice.baseui.adapter.BaseRecyclerViewAdapter
import io.agora.voice.buddy.tool.ResourcesTools

class RoomMicManagerViewHolder(binding: VoiceItemRoomMicManagerBinding) :
    BaseRecyclerViewAdapter.BaseViewHolder<VoiceItemRoomMicManagerBinding, MicManagerBean>(binding) {
    override fun binding(data: MicManagerBean?, selectedIndex: Int) {
        data?.let {
            mBinding.mtMicManagerOperate.text = it.name
            if (it.enable) {
                mBinding.mtMicManagerOperate.setTextColor(
                    ResourcesTools.getColor(context.resources, R.color.voice_main_color_156ef3)
                )
            } else {
                mBinding.mtMicManagerOperate.setTextColor(
                    ResourcesTools.getColor(context.resources, R.color.voice_dark_grey_color_979cbb)
                )
            }
        }
    }
}