package com.example.qarobot;

import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.widget.BaseAdapter;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import com.example.qarobot.entity.ChatMessage;

import org.apache.commons.lang.StringUtils;

import java.util.Date;
import java.util.List;

public class ChatMessageAdapter extends BaseAdapter {
    private Context context;
    private LayoutInflater mInflater;
    private List<ChatMessage> mDatas;
    private OnClickListener mOnClickListener;
    public ChatMessageAdapter(Context context, List<ChatMessage> datas)
    {
        this.context = context;
        mInflater = LayoutInflater.from(context);
        mDatas = datas;
        mOnClickListener = (OnClickListener) context;
    }
    @Override
    public int getCount()
    {
        return mDatas.size();
    }
    @Override
    public Object getItem(int position)
    {
        return mDatas.get(position);
    }
    @Override
    public long getItemId(int position)
    {
        return position;
    }
    /**
     * 接受到消息为1，发送消息为0
     */
    @Override
    public int getItemViewType(int position)
    {
        ChatMessage msg = mDatas.get(position);
        return msg.getType() == ChatMessage.Type.INPUT ? 1 : 0;
    }
    @Override
    public int getViewTypeCount()
    {
        return 2;
    }
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        ChatMessage chatMessage = mDatas.get(position);
        ViewHolder viewHolder = null;
        if (convertView == null)
        {
            viewHolder = new ViewHolder();
            if (chatMessage.getType() == ChatMessage.Type.INPUT)
            {
                convertView = mInflater.inflate(R.layout.main_chat_from_msg,
                        parent, false);
                viewHolder.createDate = (TextView) convertView
                        .findViewById(R.id.chat_from_createDate);
                viewHolder.content = (TextView) convertView
                        .findViewById(R.id.chat_from_content);
                convertView.setTag(viewHolder);
            } else
            {
                convertView = mInflater.inflate(R.layout.main_chat_send_msg,
                        null);
                viewHolder.createDate = (TextView) convertView
                        .findViewById(R.id.chat_send_createDate);
                viewHolder.content = (TextView) convertView
                        .findViewById(R.id.chat_send_content);
                convertView.setTag(viewHolder);
            }
        } else
        {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        final String retMsg = chatMessage.getMsg().replace(" ", "\n");
        SpannableStringBuilder spannableStringBuilder = new SpannableStringBuilder(retMsg);
        if (retMsg.contains("请选择您需要查询的对象")) {
            int startIndex = StringUtils.indexOf(retMsg, '[') + 1;
            int endIndex = StringUtils.indexOf(retMsg, ']');
            while (endIndex != -1) {
                final String ambiguousEntity = retMsg.substring(startIndex, endIndex + 1);
                spannableStringBuilder.setSpan(new ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View view) {
                        mOnClickListener.setAmbiguousEntity(ambiguousEntity);
                    }
                    @Override
                    public void updateDrawState(@NonNull TextPaint ds) {
                        super.updateDrawState(ds);
                        ds.setColor(Color.BLUE);
                    }
                }, startIndex, endIndex + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                startIndex = endIndex + 3;
                endIndex = StringUtils.indexOf(retMsg, ']', startIndex);
            }
        }
        viewHolder.content.setMovementMethod(LinkMovementMethod.getInstance());
        viewHolder.content.setHighlightColor(Color.TRANSPARENT);
        viewHolder.content.setText(spannableStringBuilder);
        viewHolder.createDate.setText(chatMessage.getDateStr());
        return convertView;
    }
    private class ViewHolder
    {
        public TextView createDate;
        public TextView name;
        public TextView content;
    }

    public interface OnClickListener {
        public void setAmbiguousEntity(String entity);
    }
}
