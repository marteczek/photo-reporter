package com.marteczek.photoreporter.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.database.entity.ForumThread;

import java.util.List;

public class ThreadListAdapter extends RecyclerView.Adapter<ThreadListAdapter.ThreadViewHolder>{

    class ThreadViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;

        private ThreadViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.thread_name);
        }
    }

    public interface OnClickListener {
        void onClick(ForumThread thread);
    }

    private final LayoutInflater inflater;

    private List<ForumThread> threads;

    private final OnClickListener onClickListener;

    ThreadListAdapter(Context context, OnClickListener onClickListener) {
        this.onClickListener = onClickListener;
        inflater = LayoutInflater.from(context);
    }

    @Override
    public @NonNull ThreadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.recyclerview_thread_item, parent, false);
        return new ThreadViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull ThreadViewHolder holder, int position) {
        if (threads != null) {
            ForumThread current = threads.get(position);
            String name = current.getName();
            if (TextUtils.isEmpty(name)) {
                name = current.getThreadId();
            }
            holder.nameTextView.setText(name);
            holder.itemView.setOnClickListener((view) -> onClickListener.onClick(current));
        } else {
            holder.nameTextView.setText(R.string.no_data);
        }
    }

    void setThreads(List<ForumThread> threads){
        this.threads = threads;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (threads != null) ? threads.size() : 0;
    }
}
