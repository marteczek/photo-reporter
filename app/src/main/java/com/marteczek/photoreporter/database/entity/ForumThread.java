package com.marteczek.photoreporter.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(tableName = "threads",
        indices={@Index(value = "thread_id", unique = true)})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class ForumThread {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @ColumnInfo(name = "thread_id")
    @NonNull
    String threadId;

    @ColumnInfo(name = "thread_path")
    String threadPath;

    String name;

    @ColumnInfo(name = "last_usage")
    Date lastUsage;
}
