package com.rizato.gameclient;

import android.content.Context;
import android.graphics.Color;
import android.support.annotation.IntDef;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.rizato.gameclient.networking.Protocol;

import java.util.ArrayList;
import java.util.List;

/**
 * This is the Adapter for the chat view. It just does colored text based on the text style
 * from the server.
 */
@SuppressWarnings("WeakerAccess")
public class ChatViewAdapter extends RecyclerView.Adapter<ChatViewAdapter.ChatViewHolder> {
    private List<Protocol.TextResponse> mList;
    private final Context mContext;
    @IntDef({PLAIN,INFO,ECHO,DAMAGE,TALK,SYSTEM,HIT,WIZ,TELL })
    public @interface TextStyle {}
    public static final int PLAIN = 0;
    public static final int INFO = 1;
    public static final int ECHO = 2;
    public static final int DAMAGE = 3;
    public static final int TALK = 4;
    public static final int SYSTEM = 5;
    public static final int HIT = 6;
    public static final int WIZ= 7;
    public static final int TELL= 8;


    public ChatViewAdapter(Context context) {
        mList = new ArrayList<>();
        mContext = context;
    }

    @SuppressWarnings("unused")
    public void setList(List<Protocol.TextResponse> list) {
        mList = list;
        notifyDataSetChanged();
    }

    public void addResponse(Protocol.TextResponse response) {
        mList.add(response);
        notifyDataSetChanged();
    }

    @Override
    public ChatViewHolder onCreateViewHolder(ViewGroup parent, @TextStyle int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.chat_simple_layout, parent, false);
        switch (viewType) {
            case TALK:
                ((TextView) view).setTextColor(Color.BLUE);
                break;
            case SYSTEM:
                ((TextView) view).setTextColor(Color.RED);
                break;

            case DAMAGE: //Intentional fall through
            case ECHO:
            case HIT:
            case INFO:
            case PLAIN:
            case TELL:
            case WIZ:
            default:
                ((TextView) view).setTextColor(Color.BLACK);
                break;
        }
        return new ChatViewHolder(view);
    }

    @Override
    public int getItemCount() {
        return mList.size();
    }

    @Override
    public int getItemViewType(int position) {
        if (mList != null && mList.size() >= position){
            return mList.get(position).style;
        }
        return -1;
    }

    @Override
    public void onBindViewHolder(ChatViewHolder holder, int position) {
        if (mList != null && holder != null) {
            holder.textView.setText(mList.get(position).message);
        }
    }

    /**
     * View holder for chat views
     */
    public class ChatViewHolder extends RecyclerView.ViewHolder {
        public final TextView textView;

        public ChatViewHolder(View itemView) {
            super(itemView);
            textView = (TextView) itemView.findViewById(android.R.id.text1);
        }
    }
}
