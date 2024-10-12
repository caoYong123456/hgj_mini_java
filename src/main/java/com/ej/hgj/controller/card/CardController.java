package com.ej.hgj.controller.card;

import com.alibaba.fastjson.JSONObject;
import com.ej.hgj.base.BaseReqVo;
import com.ej.hgj.constant.Constant;
import com.ej.hgj.controller.base.BaseController;
import com.ej.hgj.dao.active.CouponQrCodeDaoMapper;
import com.ej.hgj.dao.card.CardCstDaoMapper;
import com.ej.hgj.dao.card.CardQrCodeDaoMapper;
import com.ej.hgj.dao.config.ConstantConfDaoMapper;
import com.ej.hgj.dao.config.ProNeighConfDaoMapper;
import com.ej.hgj.dao.coupon.CouponGrantDaoMapper;
import com.ej.hgj.dao.cst.HgjCstDaoMapper;
import com.ej.hgj.dao.hu.CstIntoMapper;
import com.ej.hgj.dao.hu.HgjHouseDaoMapper;
import com.ej.hgj.dao.opendoor.OpenDoorCodeDaoMapper;
import com.ej.hgj.dao.opendoor.OpenDoorLogDaoMapper;
import com.ej.hgj.entity.active.CouponQrCode;
import com.ej.hgj.entity.card.CardCst;
import com.ej.hgj.entity.card.CardQrCode;
import com.ej.hgj.entity.config.ConstantConfig;
import com.ej.hgj.entity.config.ProNeighConfig;
import com.ej.hgj.entity.coupon.CouponGrant;
import com.ej.hgj.entity.cst.HgjCst;
import com.ej.hgj.entity.hu.CstInto;
import com.ej.hgj.entity.opendoor.OpenDoorLog;
import com.ej.hgj.enums.JiasvBasicRespCode;
import com.ej.hgj.enums.MonsterBasicRespCode;
import com.ej.hgj.utils.DateUtils;
import com.ej.hgj.utils.HttpClientUtil;
import com.ej.hgj.utils.QrCodeUtil;
import com.ej.hgj.utils.bill.TimestampGenerator;
import com.ej.hgj.vo.active.ActiveRequestVo;
import com.ej.hgj.vo.active.ActiveResponseVo;
import com.ej.hgj.vo.card.CardRequestVo;
import com.ej.hgj.vo.card.CardResponseVo;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class CardController extends BaseController {

	Logger logger = LoggerFactory.getLogger(getClass());

	@Autowired
	private CardCstDaoMapper cardCstDaoMapper;

	@Autowired
	private ProNeighConfDaoMapper proNeighConfDaoMapper;

	@Autowired
	private ConstantConfDaoMapper constantConfDaoMapper;

	@Autowired
	private HgjCstDaoMapper hgjCstDaoMapper;

	@Autowired
	private CardQrCodeDaoMapper cardQrCodeDaoMapper;

	@Autowired
	private OpenDoorLogDaoMapper openDoorLogDaoMapper;

	@Autowired
	private CstIntoMapper cstIntoMapper;

	/**
	 * 查询游泳卡信息
	 * @param baseReqVo
	 * @return
	 */
	@ResponseBody
	@RequestMapping("/card/queryCardSwim")
	public CardResponseVo couponQuery(@RequestBody BaseReqVo baseReqVo) {
		CardResponseVo cardResponseVo = new CardResponseVo();
		String proNum = baseReqVo.getProNum();
		String cstCode = baseReqVo.getCstCode();
		CardCst cardInfo = cardCstDaoMapper.getCardInfo(proNum, cstCode, "1");
		// 查询登录角色
		CstInto byWxOpenIdAndStatus_1 = cstIntoMapper.getByWxOpenIdAndStatus_1(baseReqVo.getWxOpenId());
		if(cardInfo != null && byWxOpenIdAndStatus_1 != null && (byWxOpenIdAndStatus_1.getIntoRole() == 2 || byWxOpenIdAndStatus_1.getIntoRole() == 4)){
			cardResponseVo.setCardCstId(cardInfo.getId());
			cardResponseVo.setCardCode(cardInfo.getCardCode());
			cardResponseVo.setCardName(cardInfo.getCardName());
			cardResponseVo.setCardExpNum(cardInfo.getTotalNum() - cardInfo.getApplyNum());
			cardResponseVo.setStartTime(cardInfo.getStartTime());
			cardResponseVo.setEndTime(cardInfo.getEndTime());
		}
		cardResponseVo.setRespCode(MonsterBasicRespCode.SUCCESS.getReturnCode());
		cardResponseVo.setErrCode(JiasvBasicRespCode.SUCCESS.getRespCode());
		cardResponseVo.setErrDesc(JiasvBasicRespCode.SUCCESS.getRespDesc());
		return cardResponseVo;
	}

	/**
	 * 创建游泳卡二维码
	 * @param response
	 * @param cardRequestVo
	 * @return
	 */
	@SneakyThrows
	@RequestMapping("/card/createCardQrCode")
	@ResponseBody
	public JSONObject addCouponQrCode(HttpServletResponse response, @RequestBody CardRequestVo cardRequestVo) {
		JSONObject jsonObject = new JSONObject();
		String expDate = DateUtils.strYmd(new Date());
		String cstCode = cardRequestVo.getCstCode();
		String wxOpenId = cardRequestVo.getWxOpenId();
		String proNum = cardRequestVo.getProNum();
		String cardCstId = cardRequestVo.getCardCstId();
		if(StringUtils.isBlank(cstCode) || StringUtils.isBlank(wxOpenId) ||
				StringUtils.isBlank(proNum) || StringUtils.isBlank(cardCstId)){
			jsonObject.put("RESPCODE", "999");
			jsonObject.put("ERRDESC", "请求参数错误");
			return jsonObject;
		}
		// 卡信息查询
		CardCst cardCst = cardCstDaoMapper.getById(cardCstId);
		Integer totalNum = cardCst.getTotalNum();
		Integer applyNum = cardCst.getApplyNum();
		// 卡禁用校验
		if(cardCst.getIsExp() == 0){
			jsonObject.put("RESPCODE", "999");
			jsonObject.put("ERRDESC", "卡已禁用");
			return jsonObject;
		}
		// 卡过期校验
		Integer sysTimeInt = Integer.valueOf(DateUtils.strYmd());
		Integer startTimeInt = Integer.valueOf(cardCst.getStartTime().replace("-",""));
		Integer endTimeInt = Integer.valueOf(cardCst.getEndTime().replace("-",""));
		if(sysTimeInt < startTimeInt || sysTimeInt > endTimeInt){
			jsonObject.put("RESPCODE", "999");
			jsonObject.put("ERRDESC", "卡已过期");
			return jsonObject;
		}
		// 根据日期，客户编号, 客户卡关联ID，查询已生成的二维码
		CardQrCode cardQrCodePram = new CardQrCode();
		cardQrCodePram.setExpDate(expDate);
		cardQrCodePram.setCstCode(cstCode);
		cardQrCodePram.setCardCstId(cardCstId);
		List<CardQrCode> qrCodeByExpDate = cardQrCodeDaoMapper.getQrCodeByExpDate(cardQrCodePram);
		// 如果有直接查询历史记录，反之再调用接口
		if(!qrCodeByExpDate.isEmpty()){
			CardQrCode qrCode = qrCodeByExpDate.get(0);
			// 卡二维码失效校验
			if(qrCode.getIsExp() == 0){
				jsonObject.put("RESPCODE", "999");
				jsonObject.put("ERRDESC", "当天开门次数已用完");
				return jsonObject;
			}
			String qrCodeContent = qrCode.getQrCodeContent();
			// 生成二维码
			String png_base64 = createQrCode(qrCodeContent,response);
			jsonObject.put("RESPCODE", "000");
			jsonObject.put("cardQrCode", png_base64);
			jsonObject.put("expDate",expDate);
			// 总开门次数
			ConstantConfig byKey = constantConfDaoMapper.getByKey(Constant.CARD_QR_CODE_OPEN_DOOR_SIZE);
			jsonObject.put("openDoorTotalNum", byKey.getConfigValue());
			// 已开门次数
			List<OpenDoorLog> byCardNoAndIsUnlock = openDoorLogDaoMapper.getByCardNoAndIsUnlock(qrCode.getCardNo());
			if(!byCardNoAndIsUnlock.isEmpty()) {
				jsonObject.put("openDoorApplyNum", byCardNoAndIsUnlock.size());
			}else {
				jsonObject.put("openDoorApplyNum","0");
			}
			return jsonObject;
		}
		// 游泳卡次数用完不能再创建二维码
		if((totalNum - applyNum) <= 0){
			jsonObject.put("RESPCODE", "999");
			jsonObject.put("ERRDESC", "无可用次数");
			return jsonObject;
		}
		// 拆分时间
		String[] expDateSpilt = expDate.split("-");
		Integer expYear = Integer.valueOf(expDateSpilt[0]);
		Integer expMonth = Integer.valueOf(expDateSpilt[1]);
		Integer expDay = Integer.valueOf(expDateSpilt[2]);
		// 设置特定的年、月、日、时、分、秒
		LocalDateTime startDate = LocalDateTime.of(expYear, expMonth, expDay, 00, 00, 00);
		// 设置特定的年、月、日、时、分、秒
		LocalDateTime endDate = LocalDateTime.of(expYear, expMonth, expDay, 23, 59, 59);
		// 获取时区
		ZoneId zoneId = ZoneId.systemDefault();
		// 转换为ZonedDateTime并获取毫秒时间戳
		long startTime = startDate.atZone(zoneId).toInstant().toEpochMilli();
		long endTime = endDate.atZone(zoneId).toInstant().toEpochMilli();
		// 根据项目号获取小区号
		ProNeighConfig byProjectNum = proNeighConfDaoMapper.getByProjectNum(proNum);
		String neighNo = byProjectNum.getNeighNo();
		// 根据已选择的房屋ID获取单元号
		//HgjHouse hgjHouse = hgjHouseDaoMapper.getById(houseId);
		//String unitNo = hgjHouse.getUnitNo();
		String unitNo = "1";
		// 房间号
		//String resCode = hgjHouse.getResCode();
		String resCode = "4-1-0101";
		// 截取房间号
		String[] resCodeSplit = resCode.split("-");
		String addressNumber = unitNo+resCodeSplit[2];
		// 楼层
		//String floor = hgjHouse.getFloorNum().toString();
		String floor = "1";
		// 调用获取二维码内容的接口-post请求
		ConstantConfig constantConfigUrl = constantConfDaoMapper.getByKey(Constant.OPEN_DOOR_QR_CODE_URL);
		String jsonData = "{  \"neighNo\": \"" + neighNo + "\",  \"addressNumber\": " + addressNumber + ",  \"startTime\": " +
				startTime + ",  \"endTime\": " + endTime + ",  \"unitNumber\": " + unitNo + ",  \"floors\": " + floor + "}";
		JSONObject resultJson = HttpClientUtil.sendPost(constantConfigUrl.getConfigValue(), jsonData);
		String result = resultJson.get("result").toString();
		String message = resultJson.getString("message");
		// 成功
		if("1".equals(result)){
			// 获取data中的二维码内容
			JSONObject data = resultJson.getJSONObject("data");
			String cardNo = data.get("cardNo").toString();
			String qrCodeContent = data.get("qrCode").toString();
			// 生成通行二维码
			String png_base64 = createQrCode(qrCodeContent,response);
			// 保存二维码生成记录
			CardQrCode cardQrCode = new CardQrCode();
			Date date = new Date();
			cardQrCode.setId(TimestampGenerator.generateSerialNumber());
			cardQrCode.setProNum(proNum);
			cardQrCode.setExpDate(expDate);
			cardQrCode.setStartTime(startTime);
			cardQrCode.setEndTime(endTime);
			cardQrCode.setCardNo(cardNo);
			cardQrCode.setQrCodeContent(qrCodeContent);
			cardQrCode.setNeighNo(neighNo);
			cardQrCode.setAddressNum(addressNumber);
			cardQrCode.setUnitNum(unitNo);
			cardQrCode.setFloors(floor);
			cardQrCode.setWxOpenId(wxOpenId);
			cardQrCode.setCstCode(cstCode);
			HgjCst hgjCst = hgjCstDaoMapper.getByCstCode(cstCode);
			cardQrCode.setCstName(hgjCst.getCstName());
			cardQrCode.setCardCstId(cardCstId);
			cardQrCode.setResCode(resCode);
			// 1-有效 0-无效
			cardQrCode.setIsExp(1);
			cardQrCode.setCreateTime(date);
			cardQrCode.setUpdateTime(date);
			cardQrCode.setDeleteFlag(0);
			cardQrCodeDaoMapper.save(cardQrCode);
			jsonObject.put("RESPCODE", "000");
			jsonObject.put("cardQrCode", png_base64);
			jsonObject.put("expDate",expDate);

			// 总开门次数
			ConstantConfig byKey = constantConfDaoMapper.getByKey(Constant.CARD_QR_CODE_OPEN_DOOR_SIZE);
			jsonObject.put("openDoorTotalNum", byKey.getConfigValue());
			// 已开门次数
			List<OpenDoorLog> byCardNoAndIsUnlock = openDoorLogDaoMapper.getByCardNoAndIsUnlock(cardNo);
			if(!byCardNoAndIsUnlock.isEmpty()) {
				jsonObject.put("openDoorApplyNum", byCardNoAndIsUnlock.size());
			}else {
				jsonObject.put("openDoorApplyNum","0");
			}
		}else {
			jsonObject.put("RESPCODE", "999");
			jsonObject.put("ERRDESC", message);
			return jsonObject;
		}
		return jsonObject;
	}

	@SneakyThrows
	public String createQrCode(String qrCodeContent, HttpServletResponse response){
		BufferedImage bufferedImage = QrCodeUtil.createCodeToOutputStream(qrCodeContent, response.getOutputStream());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			ImageIO.write(bufferedImage, "png", outputStream);
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] bytes = outputStream.toByteArray();
		String png_base64 = Base64.encodeBase64String(bytes);
		png_base64 = png_base64.replaceAll("\n", "").replaceAll("\r", "");//删除 \r\n
		return  png_base64;
	}


}
