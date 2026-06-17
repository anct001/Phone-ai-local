package com.phoneai.local.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.phoneai.local.R
import com.phoneai.local.model.ChatMessage
import io.noties.markwon.Markwon

class MessageAdapter : ListAdapter<ChatMessage, MessageAdapter.MessageViewHolder>(DIFF) {

    private lateinit var markwon: Markwon

    inner class MessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvContent: TextView = view.findViewById(R.id.tvContent)
        val tvCursor:  TextView = view.findViewById(R.id.tvCursor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        if (!::markwon.isInitialized) {
            markwon = Markwon.create(parent.context)
        }
        val layout = when (viewType) {
            VIEW_USER      -> R.layout.item_message_user
            VIEW_ASSISTANT -> R.layout.item_message_assistant
            else           -> R.layout.item_message_assistant
        }
        val view = LayoutInflater.from(parent.context).inflate(layout, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val msg = getItem(position)
        if (msg.role == ChatMessage.Role.ASSISTANT) {
            markwon.setMarkdown(holder.tvContent, msg.content)
        } else {
            holder.tvContent.text = msg.content
        }
        holder.tvCursor.visibility = if (msg.isStreaming) View.VISIBLE else View.GONE
    }

    override fun getItemViewType(position: Int): Int =
        if (getItem(position).role == ChatMessage.Role.USER) VIEW_USER else VIEW_ASSISTANT

    companion object {
        private const val VIEW_USER      = 0
        private const val VIEW_ASSISTANT = 1

        private val DIFF = object : DiffUtil.ItemCallback<ChatMessage>() {
            override fun areItemsTheSame(a: ChatMessage, b: ChatMessage) = a.id == b.id
            override fun areContentsTheSame(a: ChatMessage, b: ChatMessage) = a == b
        }
    }
}
