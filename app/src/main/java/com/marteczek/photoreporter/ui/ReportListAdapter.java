package com.marteczek.photoreporter.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.marteczek.photoreporter.R;
import com.marteczek.photoreporter.database.entity.Report;
import com.marteczek.photoreporter.ui.misc.ItemTouchHelperAdapter;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

public class ReportListAdapter extends RecyclerView.Adapter<ReportListAdapter.ReportViewHolder>
        implements ItemTouchHelperAdapter {

    class ReportViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView dateTextView;
        private final TextView statusTextView;

        private ReportViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.report_name);
            dateTextView = itemView.findViewById(R.id.date);
            statusTextView = itemView.findViewById(R.id.status);
        }
    }

    public interface OnClickListener {
        void onClick(Report report);
    }

    public interface OnItemDismissListener {
        void onItemDismiss(Report report);
    }

    private final LayoutInflater inflater;

    private List<Report> reports;

    private final DateFormat dateFormat;

    private final OnClickListener onClickListener;

    private final OnItemDismissListener onItemDismissListener;

    ReportListAdapter(Context context, OnClickListener onClickListener,
                      OnItemDismissListener onItemDismissListener) {
        this.onClickListener = onClickListener;
        this.onItemDismissListener = onItemDismissListener;
        inflater = LayoutInflater.from(context);
        dateFormat = SimpleDateFormat.getDateTimeInstance();
    }

    @Override
    public @NonNull ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.recyclerview_report_item, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        if (reports != null) {
            Report current = reports.get(position);
            holder.nameTextView.setText(current.getName());
            holder.dateTextView.setText(dateFormat.format(current.getDate()));
            holder.statusTextView.setText(current.getStatus());
            holder.itemView.setOnClickListener((view) -> onClickListener.onClick(current));
        } else {
            holder.nameTextView.setText("");
        }
    }

    void setReports(List<Report> reports){
        this.reports = reports;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return (reports != null) ? reports.size() : 0;
    }

    @Override
    public void onItemMove(int fromPosition, int toPosition) {}

    @Override
    public void onItemDismiss(int position) {
        if (onItemDismissListener != null) {
            onItemDismissListener.onItemDismiss(reports.get(position));
        }
    }
}
