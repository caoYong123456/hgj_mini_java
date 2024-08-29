package com.ej.hgj.entity.gonggao;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.util.Date;
import java.util.List;

@Data
public class Gonggao {

    private String id;

    private String proNum;

    private String articleId;

    private String mediaId;

    private String type;

    private String typeName;

    private String title;

    private String author;

    private String isDeleted;

    private String filePath;

    private String url;

    private String thumbUrl;

    private Integer isShow;

    private String createBy;

    private String updateBy;

    private Integer deleteFlag;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private Date updateTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    private List<String> gonggaoTypeList;

    private String content;

    // 公告阅读状态 0-未读 1-已读
    private Integer readStatus;

}
