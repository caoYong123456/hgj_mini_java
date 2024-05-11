package com.ej.hgj.dao.tag;

import com.ej.hgj.entity.cst.HgjCst;
import com.ej.hgj.entity.tag.TagCst;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Mapper
@Component
public interface TagCstDaoMapper {

    void delete(String tagId);

    void insertList(@Param("list") List<TagCst> tagCstList);

    List<TagCst> getList(TagCst tagCst);

    List<HgjCst> getCstByTagId(String tagId);


}
