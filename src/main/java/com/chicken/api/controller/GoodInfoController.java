package com.chicken.api.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chicken.api.model.AccountDetail;
import com.chicken.api.model.AccountUser;
import com.chicken.api.model.GoodInfo;
import com.chicken.api.model.GoodOrder;
import com.chicken.api.service.*;
import com.chicken.api.util.CallResult;
import com.chicken.api.util.CodeEnum;
import com.chicken.api.util.ContantUtil;
import com.chicken.api.vo.GoodInfoRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * @author zhanglei
 * @date 2019-09-07 12:00
 */
@RestController
@RequestMapping("/mp")
public class GoodInfoController extends BaseController{

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    RedisService redisService;

    @Autowired
    GoodInfoService goodInfoService;

    @Autowired
    AccountUserService accountUserService;

    @Autowired
    GoodOrderService goodOrderService;

    @Autowired
    AccountDetailService accountDetailService;

    @Autowired
    HttpServletRequest request;

    /**
     * 获取商品列表
     *
     * @return
     */
    @RequestMapping(value = "/goodInfoList", method = RequestMethod.GET)
    @ResponseBody
    public Object goodInfo() {

        List<GoodInfo> goodInfos = this.goodInfoService.selectAll();
        JSONArray jsonArray = new JSONArray();
        for (GoodInfo i : goodInfos) {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("id", i.getId());
            jsonObject.put("goodDownVirtual", i.getGoodDownVirtual());
            jsonObject.put("goodName", i.getGoodName());
            jsonObject.put("goodImg", i.getGoodImg());
            jsonObject.put("goodNum", i.getGoodNum());
            jsonObject.put("goodVirtual", i.getGoodVirtual());
            jsonObject.put("goodType", i.getGoodType());
            jsonArray.add(jsonObject);
        }
        return CallResult.success(jsonArray.toArray());
    }


    /**
     * 获取商品明细
     *
     * @return
     */
    @RequestMapping(value = "/goodDetailById", method = RequestMethod.POST)
    @ResponseBody
    public Object goodDetailById(@RequestBody GoodInfoRequest goodInfoRequest) {

        String sessionId = request.getHeader("sessionId");
        if (!isLogin(sessionId)) {
            return CallResult.fail(CodeEnum.LOGIN_OUT_TIME.getCode(), CodeEnum.LOGIN_OUT_TIME.getMsg());
        }

        if (StringUtils.isBlank(goodInfoRequest.getGoodId())) {
            return CallResult.fail(CodeEnum.LACK_PARAM.getCode(), CodeEnum.LACK_PARAM.getMsg());
        }

        GoodInfo goodInfo = this.goodInfoService.selectByPrimaryKey(Integer.valueOf(goodInfoRequest.getGoodId()));
        JSONObject goodInfoJson = new JSONObject();
        if (null != goodInfo) {
            goodInfoJson.put("id", goodInfo.getId());
            goodInfoJson.put("goodName", goodInfo.getGoodName());
            goodInfoJson.put("goodDownVirtual", goodInfo.getGoodDownVirtual());
            goodInfoJson.put("goodDetail", goodInfo.getGoodDetail());
            goodInfoJson.put("goodImg", goodInfo.getGoodImg());
            goodInfoJson.put("goodType", goodInfo.getGoodType());
            goodInfoJson.put("goodNum", goodInfo.getGoodNum());
            goodInfoJson.put("goodVirtual", goodInfo.getGoodVirtual());
            goodInfoJson.put("goodPrice", goodInfo.getGoodPrice());
        }

        return CallResult.success(goodInfoJson);
    }


    /**
     * 商品兑换
     *
     * @param goodInfoRequest
     * @return
     */
    @RequestMapping(value = "/goodExchangeById", method = RequestMethod.POST)
    @ResponseBody
    public Object goodExchangeById(@RequestBody GoodInfoRequest goodInfoRequest) {

        String sessionId = request.getHeader("sessionId");
        if (!isLogin(sessionId)) {
            return CallResult.fail(CodeEnum.LOGIN_OUT_TIME.getCode(), CodeEnum.LOGIN_OUT_TIME.getMsg());
        }

        if (StringUtils.isBlank(goodInfoRequest.getGoodId()) || StringUtils.isBlank(goodInfoRequest.getOpenid())
                || StringUtils.isBlank(goodInfoRequest.getScore())) {
            return CallResult.fail(CodeEnum.LACK_PARAM.getCode(), CodeEnum.LACK_PARAM.getMsg());
        }

        Object userId = redisService.get(ContantUtil.OPEN_ID.concat(goodInfoRequest.getOpenid()));
        if (null == userId) {
            return CallResult.fail(CodeEnum.NO_FIND_USER.getCode(), CodeEnum.NO_FIND_USER.getMsg());
        } else {
            goodInfoRequest.setUserId(userId.toString());
        }

        //缓存获得产品数量
        Object goodNum = redisService.get("good:id:".concat(goodInfoRequest.getGoodId()));
        if (null == goodNum) {
            return CallResult.fail(CodeEnum.GOOD_NO_EXITS.getCode(), CodeEnum.GOOD_NO_EXITS.getMsg());
        }

        //判断用户积分是否大于商品积分
        if (!userIntegral(goodInfoRequest.getUserId(), Double.valueOf(goodInfoRequest.getScore()))) {
            return CallResult.fail(CodeEnum.INTEGRAL_NO_THOUGH.getCode(), CodeEnum.INTEGRAL_NO_THOUGH.getMsg());
        }

        if (Integer.valueOf(goodNum.toString()) > 0) {
            //数字减一
            redisService.increment("good:id:".concat(goodInfoRequest.getGoodId()), -1);

            //减少库存
            if (!decrease(Integer.valueOf(goodInfoRequest.getGoodId()), goodInfoRequest.getUserId())) {
                logger.info("商品兑换，用户{}兑换商品{}，库存不足。", goodInfoRequest.getUserId(), goodInfoRequest.getGoodId());
                return CallResult.fail(CodeEnum.GOOD_NO_THOUGH.getCode(), CodeEnum.GOOD_NO_THOUGH.getMsg());
            }

            insertGoodOrder(Double.valueOf(goodInfoRequest.getScore()), Integer.valueOf(goodInfoRequest.getGoodId()), Integer.valueOf(goodInfoRequest.getUserId()), goodInfoRequest.getOpenid());
        }

        return CallResult.success();
    }


    /**
     * 减少库存
     *
     * @param goodId
     * @return
     */
    private boolean decrease(Integer goodId, String userId) {

        GoodInfo goodInfo = this.goodInfoService.selectByPrimaryKey(goodId);
        if (null == goodInfo) {
            return false;
        }

        if (goodInfo.getGoodNum() <= 0) {
            return false;
        }

        goodInfo.setGoodNum(goodInfo.getGoodNum() - 1);
        goodInfoService.updateByPrimaryKey(goodInfo);
        logger.info("商品兑换，减少商品库存，原商品数据量{},兑换用户{}", goodInfo.getGoodNum() + 1, userId);

        return true;
    }

    /**
     * 判断自己的积分是否大于兑换积分
     *
     * @param userId
     * @param score
     * @return
     */
    private boolean userIntegral(String userId, Double score) {

        Double myscore = redisService.score(ContantUtil.USER_RANKING_LIST, userId);
        if (myscore >= score) {
            return true;
        }

        return false;
    }

    public void insertGoodOrder(Double score, Integer goodId, Integer userId, String openid) {

        //减少积分
        AccountUser accountUser = this.accountUserService.selectByPrimaryKey(userId);
        if (null != accountUser) {
            accountUser.setBalance(accountUser.getBalance() - score);
            accountUser.setConsumeCount(accountUser.getConsumeCount() + score);
            accountUserService.updateByPrimaryKey(accountUser);
            logger.info("商品兑换，用户id{}，商品id:{},消耗积分{}", userId, goodId, score);
            //修改排行榜分值
            redisService.incrScore(ContantUtil.USER_RANKING_LIST, accountUser.getUserId().toString(), -score);

            //修改自己排行榜的分
            redisService.incrScore(ContantUtil.FRIEND_RANKING_LIST.concat(openid),accountUser.getUserId().toString(),-score);

            //修改好友排行榜分值
            Object myFriend = redisService.get(ContantUtil.USER_OWNER_SET.concat(openid));
            if (null != myFriend) {
                redisService.incrScore(myFriend.toString(), accountUser.getUserId().toString(), -score);
            }
        }

        //记录订单
        GoodOrder goodOrder = new GoodOrder();
        goodOrder.setCreateTime(new Date());
        goodOrder.setExchangeStatus("1");
        goodOrder.setExchangeTime(new Date());
        goodOrder.setGoodId(goodId);
        UUID uuid = UUID.randomUUID();
        goodOrder.setOrderNum(uuid.toString().replace("-", ""));
        goodOrder.setStatus("1");
        goodOrder.setUserId(userId);
        this.goodOrderService.insert(goodOrder);
        logger.info("商品兑换，用户id{}，商品id:{},消耗积分{},插入到订单表", userId, goodId, score);

        //记录明细
        AccountDetail signed = new AccountDetail();
        signed.setUserId(userId);
        signed.setCreateTime(new Date());
        signed.setDetailFlag(4);
        signed.setDetailType("商品兑换");
        signed.setScoreCount(accountUser.getBalance());
        signed.setStatus("1");
        signed.setScore(-score);
        signed.setRemark(uuid.toString().replace("-", ""));
        this.accountDetailService.insert(signed);
        logger.info("商品兑换，用户id{}，商品id:{},消耗积分{},插入到流水表", userId, goodId, -score);
    }

    @RequestMapping(value = "/test", method = RequestMethod.GET)
    @ResponseBody
    public Object test() {


        Set<Object> a = redisService.revRange(ContantUtil.USER_RANKING_LIST, 0, 10000);
        Iterator<Object> it = a.iterator();
        while (it.hasNext()) {
            String str = it.next().toString();
            System.out.println(str);
            System.out.println("排名：" + redisService.rank(ContantUtil.USER_RANKING_LIST, str));
            System.out.println("分值：" + redisService.score(ContantUtil.USER_RANKING_LIST, str));


        }
        System.out.println("===========================");

        return "success";
    }
}