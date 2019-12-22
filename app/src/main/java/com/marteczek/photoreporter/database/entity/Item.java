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

@Entity(tableName = "items",
    foreignKeys = {
        @ForeignKey(entity = Report.class, parentColumns = "id", childColumns = "id_report"),
        @ForeignKey(entity = Post.class, parentColumns = "id", childColumns = "id_post")},
    indices = {@Index("id_report"), @Index("id_post")})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Item {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    private String header;

    @ColumnInfo(name = "picture_path")
    private String picturePath;

    @ColumnInfo(name = "picture_url")
    private String pictureUrl;

    @ColumnInfo(name = "picture_uri")
    private String pictureUri;

    @ColumnInfo(name = "picture_rotation")
    private int pictureRotation;

    @ColumnInfo(name = "thumbnail_path")
    private String thumbnailPath;

    @ColumnInfo(name = "thumbnail_required_width")
    int thumbnailRequiredWidth;

    @ColumnInfo(name = "thumbnail_required_height")
    int thumbnailRequiredHeight;

    @ElementStatus
    private String status;

    @ColumnInfo(name = "id_report")
    private Long reportId;

    @ColumnInfo(name = "id_post")
    private Long postId;

    @ColumnInfo(name="host_metadata")
    private String hostMetadata;

    private Long succession;
}
