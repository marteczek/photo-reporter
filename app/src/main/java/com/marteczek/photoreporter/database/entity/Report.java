package com.marteczek.photoreporter.database.entity;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.marteczek.photoreporter.database.entity.type.ReportStatus;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity(tableName = "reports")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class Report {

    @PrimaryKey(autoGenerate = true)
    private Long id;

    @NonNull
    private String name;

    private Date date;

    @ColumnInfo(name="thread_id")
    private String threadId;

    @ReportStatus
    private String status;

    @ColumnInfo(name="picture_host")
    private String pictureHost;

    @ColumnInfo(name="host_metadata")
    private String hostMetadata;

    //TODO test this
    public static class MessageBuilder {

        @ReportStatus
        private String status;

        //replaces the Lombok method
        public MessageBuilder status(@ReportStatus String status) {
            this.status = status;
            return this;
        }
    }
}
