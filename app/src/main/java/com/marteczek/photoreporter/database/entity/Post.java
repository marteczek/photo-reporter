package com.marteczek.photoreporter.database.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.marteczek.photoreporter.database.entity.type.ElementStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(tableName = "posts",
    foreignKeys =
        @ForeignKey(entity = Report.class, parentColumns = "id", childColumns = "id_report"),
    indices =
        @Index("id_report"))
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Post {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    private Long number;

    @ElementStatus
    private String status;

    @ColumnInfo(name = "id_report")
    private Long reportId;

    @ColumnInfo(name = "generated_content")
    private String generatedContent;

    private String content;
}
