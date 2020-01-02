package com.marteczek.photoreporter.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.database.entity.ForumThread;

import java.util.List;

import static androidx.recyclerview.widget.ItemTouchHelper.Callback.makeMovementFlags;

public class ThreadListAdapter extends RecyclerView.Adapter<ThreadListAdapter.ThreadViewHolder>
        implements ThreadListActivity.ItemTouchHelperAdapter {

    private final LayoutInflater inflater;

    private List<ForumThread> threads;

    private List<String> threadsIdsInReports;

    private final OnClickListener onClickListener;


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

    void setThreadsIdsInReports(List<String> threadsIds) {
        this.threadsIdsInReports = threadsIds;
    }

    @Override
    public int getItemCount() {
        return (threads != null) ? threads.size() : 0;
    }


    @Override
    public int getMovementFlags(int position) {
        if (threads != null && threadsIdsInReports != null
                && !threadsIdsInReports.contains(threads.get(position).getThreadId())) {
            return makeMovementFlags(0, ItemTouchHelper.END);
        }
        return 0;
    }

    @Override
    public String onItemDismiss(int position) {
        String  threadId = null;
        if (threads != null) {
            threadId = threads.get(position).getThreadId();
            threads.remove(position);
        }
        return threadId;
    }
}
