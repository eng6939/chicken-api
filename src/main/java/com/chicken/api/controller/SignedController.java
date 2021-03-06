package com.chicken.api.controller;

import com.alibaba.fastjson.JSONObject;
import com.chicken.api.model.AccountSigned;
import com.chicken.api.model.AccountUser;
import com.chicken.api.service.AccountSignedService;
import com.chicken.api.service.AccountUserService;
import com.chicken.api.service.RedisService;
import com.chicken.api.util.CallResult;
import com.chicken.api.util.CodeEnum;
import com.chicken.api.util.ContantUtil;
import com.chicken.api.util.DateUtil;
import com.chicken.api.vo.SignedRequest;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * @author zhanglei
 * @date 2019-09-07 10:27
 */
@RestController
@RequestMapping("/mp")
public class SignedController extends BaseController {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    AccountSignedService accountSignedService;

    @Autowired
    AccountUserService accountUserService;

    @Autowired
    RedisService redisService;

    @Autowired
    HttpServletRequest request;


    @RequestMapping(value = "/signed", method = RequestMethod.POST)
    @ResponseBody
    public Object singed(@RequestBody SignedRequest signedRequest) {

        String sessionId = request.getHeader("sessionId");
        if (!isLogin(sessionId)) {
            return CallResult.fail(CodeEnum.LOGIN_OUT_TIME.getCode(), CodeEnum.LOGIN_OUT_TIME.getMsg());
        }

        if (StringUtils.isBlank(signedRequest.getOpenid())) {
            return CallResult.fail(CodeEnum.LACK_PARAM.getCode(), CodeEnum.LACK_PARAM.getMsg());
        }

        Object userId = redisService.get(ContantUtil.OPEN_ID.concat(signedRequest.getOpenid()));
        if (null == userId) {
            return CallResult.fail(CodeEnum.NO_FIND_USER.getCode(), CodeEnum.NO_FIND_USER.getMsg());
        } else {
            signedRequest.setUserId(userId.toString());
        }

        //当期那时间
        String now = DateUtil.getSpecifiedDay("yyyy-MM-dd", 0);

        //获取缓存判断是否打卡
        Object isSigned = redisService.get(ContantUtil.SIGNED_KEY.concat(now).concat(":").concat(signedRequest.getUserId()));
        if (null != isSigned) {
            return returnResult(isSigned.toString());
        }

        //判断缓存打卡天数是否有值，如果存在分值获取
        String yesterday = DateUtil.getSpecifiedDay("yyyy-MM-dd", 1);
        Object yesterDayObj = redisService.get(ContantUtil.SIGNED_KEY.concat(yesterday).concat(":").concat(signedRequest.getUserId()));
        logger.info("签到打卡，昨日是否打卡={}", yesterday);
        if (null == yesterDayObj) {
            //删除原来的key
            redisService.deleteKey(ContantUtil.TOTAL_KEY.concat(signedRequest.getUserId()));
            //获取第一天的分值，打卡
            Object oneday = redisService.get("d:oneday");
            insertCache(now, signedRequest.getUserId(), oneday.toString());
            insertDetail(oneday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(oneday.toString());
        }

        //获取已经打卡几天
        Object total = redisService.get(ContantUtil.TOTAL_KEY.concat(signedRequest.getUserId()));
        logger.info("签到打卡，已经打卡天数={}", total);
        if (total.toString().equals("1")) {
            //获取第二天的积分
            Object twoDay = redisService.get("d:twoday");
            insertCache(now, signedRequest.getUserId(), twoDay.toString());
            insertDetail(twoDay.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(twoDay.toString());
        } else if (total.toString().equals("2")) {
            //获取第三天的积分
            Object threeday = redisService.get("d:threeday");
            insertCache(now, signedRequest.getUserId(), threeday.toString());
            insertDetail(threeday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(threeday.toString());
        } else if (total.toString().equals("3")) {
            //获取第四天的积分
            Object fourday = redisService.get("d:fourday");
            insertCache(now, signedRequest.getUserId(), fourday.toString());
            insertDetail(fourday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(fourday.toString());
        } else if (total.toString().equals("4")) {
            //获取第五天的积分
            Object fiveday = redisService.get("d:fiveday");
            insertCache(now, signedRequest.getUserId(), fiveday.toString());
            insertDetail(fiveday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(fiveday.toString());
        } else if (total.toString().equals("5")) {
            //获取第六天的积分
            Object sixday = redisService.get("d:sixday");
            insertCache(now, signedRequest.getUserId(), sixday.toString());
            insertDetail(sixday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(sixday.toString());
        } else if (total.toString().equals("6")) {
            //获取第六天的积分
            Object sevenday = redisService.get("d:sevenday");
            insertCache(now, signedRequest.getUserId(), sevenday.toString());
            insertDetail(sevenday.toString(), signedRequest.getUserId(), signedRequest.getOpenid());
            return returnResult(sevenday.toString());
        }
        return returnResult("2");
    }

    /**
     * 插入缓存
     *
     * @param now
     * @param userId
     * @param score
     */
    private void insertCache(String now, String userId, String score) {
        logger.info("now{}, userId{}, score{}", now, userId, score);
        redisService.setByTime(ContantUtil.SIGNED_KEY.concat(now).concat(":").concat(userId), score, 2, TimeUnit.DAYS);
        redisService.increment(ContantUtil.TOTAL_KEY.concat(userId), 1);
        redisService.increment(ContantUtil.USER_TOTAL_KEY.concat(userId), 1);
    }

    public void insertDetail(String score, String userId, String openid) {
        //查询用户账户信息
        AccountUser accountUser = this.accountUserService.selectByUserId(Integer.valueOf(userId));
        if (null != accountUser) {
            accountUser.setBalance(accountUser.getBalance() + Double.valueOf(score));
            accountUser.setAttentCount(accountUser.getAttentCount() + Double.valueOf(score));
            accountUserService.updateByPrimaryKey(accountUser);
            logger.info("用户打卡，用户id{}，用户打卡得分{}", userId, score);

            //修改排行榜分值
            redisService.incrScore(ContantUtil.USER_RANKING_LIST, accountUser.getUserId().toString(), Double.valueOf(score));

            //修改自己排行榜的分
            redisService.incrScore(ContantUtil.FRIEND_RANKING_LIST.concat(openid), accountUser.getUserId().toString(), Double.valueOf(score));

            //修改好友排行榜分值
            Object myFriend = redisService.get(ContantUtil.USER_OWNER_SET.concat(openid));
            if (null != myFriend) {
                redisService.incrScore(myFriend.toString(), accountUser.getUserId().toString(), Double.valueOf(score));
            }
        }

        //插入到表
        insertSigned(Integer.valueOf(userId), Double.valueOf(score), accountUser.getAttentCount());
    }


    /**
     * 插入到记录
     *
     * @param userId
     * @param score
     * @param count
     */
    private void insertSigned(Integer userId, Double score, Double count) {
        AccountSigned signed = new AccountSigned();
        signed.setUserId(userId);
        signed.setCreateTime(new Date());
        signed.setDetailFlag(1);
        signed.setDetailType("打卡");
        signed.setScoreCount(count);
        signed.setStatus("1");
        signed.setScore(score);
        signed.setSignedTime(new Date());
        signed.setRemark("用户打卡");
        this.accountSignedService.insert(signed);
        logger.info("用户打卡，用户id{},打卡时间{}", userId, DateUtil.getNow());
    }

    private CallResult returnResult(String isSigned) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("score", isSigned);
        return CallResult.success(jsonObject);
    }
}
