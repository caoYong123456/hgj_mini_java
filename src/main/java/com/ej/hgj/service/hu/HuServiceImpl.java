package com.ej.hgj.service.hu;

import com.alibaba.fastjson.JSONObject;
import com.ej.hgj.constant.Constant;
import com.ej.hgj.dao.hu.CstIntoHouseDaoMapper;
import com.ej.hgj.dao.hu.CstIntoMapper;
import com.ej.hgj.dao.role.RoleDaoMapper;
import com.ej.hgj.entity.hu.CstInto;
import com.ej.hgj.entity.hu.CstIntoHouse;
import com.ej.hgj.entity.role.Role;
import com.ej.hgj.request.hu.HuCheckInRequest;
import com.ej.hgj.service.role.RoleService;
import com.ej.hgj.vo.hu.HouseInfoVO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
@Transactional
public class HuServiceImpl implements HuService {

    Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    private CstIntoHouseDaoMapper cstIntoHouseDaoMapper;

    @Autowired
    private CstIntoMapper cstIntoMapper;

    @Override
    public JSONObject updateIntoStatus(JSONObject jsonObject, HuCheckInRequest huCheckInRequest, CstInto cstInto) {
        // 根据微信号与入住状态 1-已入住(客户、产权人) 3-待审核(委托人、住户)查出一条入住信息
        CstInto cstIntoByWxOpenId = cstIntoMapper.getByWxOpenIdAndStatus_1_3(huCheckInRequest.getWxOpenId());
        // 入住角色
        Integer intoRole = cstInto.getIntoRole();
        if(cstIntoByWxOpenId == null){
            // 根据角色更新入住状态为已入住 客户-0 与产权人-2
            if(intoRole == Constant.INTO_ROLE_CST || intoRole == Constant.INTO_ROLE_PROPERTY_OWNER){
                // 更新为已入住
                CstInto cInto = new CstInto();
                cInto.setId(huCheckInRequest.getCstIntoId());
                cInto.setWxOpenId(huCheckInRequest.getWxOpenId());
                cInto.setUserName(huCheckInRequest.getUserName());
                cInto.setPhone(huCheckInRequest.getPhone());
                cInto.setIntoStatus(Constant.INTO_STATUS_Y);
                cInto.setUpdateTime(new Date());
                cstIntoMapper.update(cInto);
                jsonObject.put("respCode", Constant.SUCCESS);
                logger.info("---------------"+huCheckInRequest.getCstCode()+"客户或者产权人入住成功-----------------");
                // 根据角色更新入住状态为待审核 委托人-1 与 住户-3
            }else if(cstInto.getIntoRole() == Constant.INTO_ROLE_ENTRUST  || cstInto.getIntoRole() == Constant.INTO_ROLE_HOUSEHOLD){
                // 更新用户入住状态为待审核
                CstInto cInto = new CstInto();
                cInto.setId(huCheckInRequest.getCstIntoId());
                cInto.setWxOpenId(huCheckInRequest.getWxOpenId());
                cInto.setUserName(huCheckInRequest.getUserName());
                cInto.setPhone(huCheckInRequest.getPhone());
                cInto.setIntoStatus(Constant.INTO_STATUS_A);
                cInto.setUpdateTime(new Date());
                cstIntoMapper.update(cInto);
                // 同时更新入住房间表
                CstIntoHouse cstIntoHouse = new CstIntoHouse();
                cstIntoHouse.setCstIntoId(huCheckInRequest.getCstIntoId());
                cstIntoHouse.setIntoStatus(Constant.INTO_STATUS_A);
                cstIntoHouse.setUpdateTime(new Date());
                cstIntoHouseDaoMapper.updateByCstIntoId(cstIntoHouse);
                jsonObject.put("respCode", Constant.SUCCESS);
                logger.info("---------------"+huCheckInRequest.getCstCode()+"委托人或者住户入住成功-----------------");
            }
        }else {
            jsonObject.put("respCode", Constant.FAIL_RESULT_CODE);
            jsonObject.put("errDesc", "请勿重复绑定!");
        }

        return jsonObject;
    }

    @Override
    public void updateStatus(HouseInfoVO houseInfoVO) {
        // 同意
        if("agree".equals(houseInfoVO.getButtonType())){
            CstInto cstInto = new CstInto();
            cstInto.setId(houseInfoVO.getId());
            cstInto.setIntoStatus(Constant.INTO_STATUS_Y);
            cstInto.setUpdateTime(new Date());
            cstIntoMapper.update(cstInto);

            CstIntoHouse cstIntoHouse = new CstIntoHouse();
            cstIntoHouse.setId(houseInfoVO.getCstIntoHouseId());
            cstIntoHouse.setIntoStatus(Constant.INTO_STATUS_Y);
            cstIntoHouse.setUpdateTime(new Date());
            cstIntoHouseDaoMapper.updateById(cstIntoHouse);

            // 拒绝，移除
        }else if("refuse".equals(houseInfoVO.getButtonType()) || "remove".equals(houseInfoVO.getButtonType())){
            CstIntoHouse cstIntoHouse = new CstIntoHouse();
            cstIntoHouse.setId(houseInfoVO.getCstIntoHouseId());
            cstIntoHouse.setIntoStatus(Constant.INTO_STATUS_U);
            cstIntoHouse.setUpdateTime(new Date());
            cstIntoHouseDaoMapper.updateById(cstIntoHouse);

            // 如果住户、委托人绑定房间被全部解除，入住表也解除
            CstInto cs = cstIntoMapper.getById(houseInfoVO.getId());
            List<CstIntoHouse> cstIntoHouseList = cstIntoHouseDaoMapper.getByCstIntoIdAndIntoStatus(houseInfoVO.getId());
            if(cstIntoHouseList.isEmpty() && cs != null && (cs.getIntoRole() == Constant.INTO_ROLE_ENTRUST || cs.getIntoRole() == Constant.INTO_ROLE_HOUSEHOLD)){
                CstInto cstInto = new CstInto();
                cstInto.setId(houseInfoVO.getId());
                cstInto.setIntoStatus(Constant.INTO_STATUS_U);
                cstInto.setUpdateTime(new Date());
                cstIntoMapper.update(cstInto);
            }
        }
    }
}
