package com.example.qarobot;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.view.View;
import android.view.Window;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.qarobot.entity.ChatMessage;
import com.example.qarobot.util.HttpUtils;

import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ChatMessageAdapter.OnClickListener{

    public static final int REQUEST_VOICE = 1;

    private static String lastQuestion;
    /**
     * 展示消息的listview
     */
    private ListView mChatView;
    /**
     * 文本域
     */
    private EditText mMsg;
    /**
     * 存储聊天消息
     */
    private List<ChatMessage> mDatas = new ArrayList<ChatMessage>();

    private Button mVoiceButton;
    /**
     * 适配器
     */
    private ChatMessageAdapter mAdapter;
    private Handler mHandler = new Handler()
    {
        public void handleMessage(android.os.Message msg)
        {
            ChatMessage from = (ChatMessage) msg.obj;
            mDatas.add(from);
            mAdapter.notifyDataSetChanged();
            mChatView.setSelection(mDatas.size() - 1);
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.main_chatting);
        initView();
        mAdapter = new ChatMessageAdapter(this, mDatas);
        mChatView.setAdapter(mAdapter);
    }
    private void initView() {
        mChatView = (ListView) findViewById(R.id.id_chat_listView);
        mMsg = (EditText) findViewById(R.id.id_chat_msg);
        mDatas.add(new ChatMessage(ChatMessage.Type.INPUT, "我是一休，很高兴为您服务"));
        mVoiceButton =(Button) findViewById(R.id.id_chat_voice);
        mVoiceButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, RecogActivity.class);
                startActivityForResult(intent, REQUEST_VOICE);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        if (requestCode == REQUEST_VOICE) {
            String returnedData = data.getStringExtra("data_return").trim();
            returnedData = returnedData.substring(6, returnedData.length());
            mMsg.setText(returnedData);
        }
    }

    public void sendMessage(View view) {
        final String msg = mMsg.getText().toString();
        if (TextUtils.isEmpty(msg)) {
            Toast.makeText(this, "您还没有输入文字...", Toast.LENGTH_SHORT).show();
            return;
        }
        ChatMessage to = new ChatMessage(ChatMessage.Type.OUTPUT, msg);
        to.setDate(new Date());
        mDatas.add(to);
        mAdapter.notifyDataSetChanged();
        mChatView.setSelection(mDatas.size() - 1);
        mMsg.setText("");
        // 关闭软键盘
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        // 得到InputMethodManager的实例
        if (imm.isActive()) {
            // 如果开启
            imm.toggleSoftInput(InputMethodManager.SHOW_IMPLICIT,
                    InputMethodManager.HIDE_NOT_ALWAYS);
            // 关闭软键盘，开启方法相同，这个方法是切换开启与关闭状态的
        }
        new Thread()
        {
            public void run()
            {
                ChatMessage from = null;
                try {
                    // 问题是完整的句子
                    if (!msg.contains("[")) {
                        from = HttpUtils.sendMsg(msg);
                        lastQuestion = msg;
                        String ans = from.getMsg();
                        if (ans.contains("抱歉")) {
                            ans = ans + "\n您可以点击下面的链接前往百度搜索：\nhttps://www.baidu.com/baidu?tn=monline_3_dg&ie=utf-8&wd="  + lastQuestion;
                            from.setMsg(ans);
                        }
                    } else {
                        // 问题是一个精确实体，先找出精确实体对应的模糊实体，然后将原来问题中的模糊实体替换为精确实体
                        int endIndex = StringUtils.indexOf(msg, "[");
                        String entity = msg.substring(0, endIndex).trim();
                        String lastQuestionDisambiguated = null;
                        // 原问题中如果只有西方人姓名的一部分，则将其替换为完整的姓名，例如将“乔布斯”替换为“史蒂夫·乔布斯”
                        if (entity.contains("·")) {
                            String[] subEntityArray = entity.split("·");
                            for (int i = 0; i < subEntityArray.length; i++) {
                                String subEntity = subEntityArray[i];
                                if (lastQuestion.contains(subEntity)) {
                                    lastQuestionDisambiguated = lastQuestion.replace(subEntity, msg);
                                }
                            }
                        } else {
                            lastQuestionDisambiguated = lastQuestion.replace(entity, msg);
                        }
                        from = HttpUtils.sendMsg(lastQuestionDisambiguated);
                        // 解决TextView autoLink属性无法识别方括号的问题，把方括号的内容提到实体前面
                        String description = msg.substring(endIndex + 1, msg.length() - 1).trim();
                        String entityWithDescription = description + entity;
                        String questionForWebSearch = lastQuestion.replace(entity, entityWithDescription);
                        String ans = from.getMsg();
                        if (ans.contains("抱歉")) {
                            ans = ans + "\n您可以点击下面的链接前往百度搜索：\nhttps://www.baidu.com/baidu?tn=monline_3_dg&ie=utf-8&wd=" + questionForWebSearch;
                            from.setMsg(ans);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    from = new ChatMessage(ChatMessage.Type.INPUT, "服务器挂了呢...");
                }
                Message message = Message.obtain();
                message.obj = from;
                mHandler.sendMessage(message);
            };
        }.start();
    }

    @Override
    public void setAmbiguousEntity(String entity) {
        mMsg.setText(entity);
    }
}
