package com.chicken.api.service;

import com.alibaba.fastjson.JSONObject;
import com.chicken.api.util.ContantUtil;
import com.chicken.api.util.WechatUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * @description: redis包装类
 * @author: zhanglei
 * @create: 2019-09-02 19:11
 **/
@Service(value = "redisService")
public class RedisService {

    private Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    RedisTemplate<String, Object> redisTemplate;


    public void set(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
        logger.info("set cache key {}  value {}", key, value);
    }


    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }


    public void setByTime(String key, Object value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().set(key, value, time, timeUnit);
        logger.info("setByTime cache key {}  value {} time {} timeUnit {}", key, value, time, timeUnit);
    }


    public void increment(String key, long value) {
        redisTemplate.opsForValue().increment(key, value);
        logger.info("increment cache key {}  value {}", key, value);
    }


    public void increment(String key, long value, long time, TimeUnit timeUnit) {
        redisTemplate.opsForValue().increment(key, value);
        redisTemplate.expire(key, time, timeUnit);
        logger.info("increment cache key {}  value {} time {} timeUnit {}", key, value, time, timeUnit);
    }

    public void deleteKey(String key) {
        redisTemplate.delete(key);
        logger.info("delete key {}", key);
    }

    public void setSortSet(String key, String value, Double score) {
        redisTemplate.opsForZSet().add(key, value, score);
        logger.info(" sortSet set key{},value{},score{}", key, value, score);
    }

    /**
     * 查询集合中指定顺序的值  zrevrange
     *
     * 返回有序的集合中，score大的在前面
     *
     * @param key
     * @param start
     * @param end
     * @return
     */
    public Set<Object> revRange(String key, Integer start, Integer end) {
        return redisTemplate.opsForZSet().reverseRange(key, start, end);
    }

    /**
     * 判断value在zset中的排名  zrank
     *
     * @param key
     * @param value
     * @return
     */
    public Long rank(String key, String value) {
        logger.info(" sortSet set key{},value{}", key, value);
        return  redisTemplate.opsForZSet().reverseRank(key, value);
    }

    /**
     * 查询value对应的score   zscore
     *
     * @param key
     * @param value
     * @return
     */
    public Double score(String key, String value) {
        return redisTemplate.opsForZSet().score(key, value);
    }

    /**
     * score的增加or减少 zincrby
     *
     * @param key
     * @param value
     * @param score
     */
    public Double incrScore(String key, String value, double score) {
        return redisTemplate.opsForZSet().incrementScore(key, value, score);
    }

    /**
     * 删除元素 zrem
     *
     * @param key
     * @param value
     */
    public void remove(String key, String value) {
        redisTemplate.opsForZSet().remove(key, value);
    }

    /**
     * 初始化0
     *两次放入队列，保证成功率
     * @param key
     */
    public long leftPush(String key,String value) {
        Long listCnt;
        try {
            listCnt = redisTemplate.opsForList().leftPush(key, value);
        } catch (Exception e) {
            logger.warn(e.getMessage());
            listCnt = redisTemplate.opsForList().leftPush(key, value);
        }
        return listCnt;
    }

    /**
     * 取数据
     * @param key
     * @return
     */
    public Object rightPop(String key){
        return redisTemplate.opsForList().rightPop(key);
    }


    /**
     * 功能描述
     *
     * @param openId  小程序openId
     * @param formId  小程序formId
     * @param content 通知内容
     * @param template 通知内容
     * @return boolean
     * @author zhanglei
     * @date 2019/09/14
     *
     * 兑换项目
     * {{keyword1.DATA}}
     *
     * 兑换时间
     * {{keyword2.DATA}}
     *
     * 订单编号
     * {{keyword3.DATA}}
     *
     * 收货地址
     * {{keyword4.DATA}}
     *
     * 备注信息
     * {{keyword5.DATA}}
     */
    public boolean pushExchangeSuccessNotice(String openId, String formId, String content,String template) {
        logger.info("兑换商品成功推送消息方法开始，openid={},content={}",openId,content);
        //缓存access_token
        String access_token = getAccessToken();

        String[] vals= content.split("@");

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("touser", openId);
        // DINING_TEMPLATE 模板Id  微信公众平台添加模板时生成的ID
        jsonObject1.put("template_id", template);
        jsonObject1.put("form_id", formId);
        JSONObject key = new JSONObject();
        JSONObject val = new JSONObject();

        //兑换项目
        val.put("value", vals[0]);
        key.put("keyword1", val);

        //兑换时间
        val = new JSONObject();
        val.put("value", vals[1]);
        key.put("keyword2", val);

        //订单编号
        val = new JSONObject();
        val.put("value", vals[2]);
        key.put("keyword3", val);

        //收货地址
        val = new JSONObject();
        val.put("value", vals[3]);
        key.put("keyword4", val);

        //备注信息
        val = new JSONObject();
        val.put("value", vals[4]);
        key.put("keyword5", val);


        jsonObject1.put("data", key);
        logger.info("兑换商品成功推送消息方法，推送json={}",jsonObject1.toJSONString());
        boolean pushResult = WechatUtil.setPush(jsonObject1.toString(), access_token);
        logger.info("兑换商品成功推送消息方法结束：推送结果" + pushResult);
        return pushResult;
    }


    private String getAccessToken(){
        Object ac = get(ContantUtil.ACCESS_TOKEN);
        String access_token = "";
        if (null == ac) {
            JSONObject jsonObject = WechatUtil.getAccessToken();
            if (jsonObject.get("expires_in") != null && jsonObject.get("expires_in").toString() != ""
                    && Integer.parseInt(jsonObject.get("expires_in").toString()) == 7200) {
                setByTime(ContantUtil.ACCESS_TOKEN, jsonObject.get("access_token"), 2, TimeUnit.HOURS);
                access_token = jsonObject.getString("access_token");
            }
        } else {
            access_token = ac.toString();
        }
        return access_token;
    }

    /**
     * 功能描述
     *
     * @param openId  小程序openId
     * @param formId  小程序formId
     * @param content 通知内容
     * @param template 模板ID
     * @return boolean
     * @author zhanglei
     * @date 2019/09/14
     *物品名称
     * {{keyword1.DATA}}
     *
     * 快递公司
     * {{keyword2.DATA}}
     *
     * 发货时间
     * {{keyword3.DATA}}
     *
     * 快递单号
     * {{keyword4.DATA}}
     *
     */
    public boolean pushSendExpressNotice(String openId, String formId, String content,String template) {
        logger.info("快递发货推送消息方法开始，openid={},content={}",openId,content);
        //缓存access_token
        String access_token = getAccessToken();

        String[] vals= content.split("@");

        JSONObject jsonObject1 = new JSONObject();
        jsonObject1.put("touser", openId);
        // DINING_TEMPLATE 模板Id  微信公众平台添加模板时生成的ID
        jsonObject1.put("template_id", template);
        jsonObject1.put("form_id", formId);
        JSONObject key = new JSONObject();
        JSONObject val = new JSONObject();

        //物品名称
        val.put("value", vals[0]);
        key.put("keyword1", val);

        //快递公司
        val = new JSONObject();
        val.put("value", vals[1]);
        key.put("keyword2", val);

        //发货时间
        val = new JSONObject();
        val.put("value", vals[2]);
        key.put("keyword3", val);

        //快递单号
        val = new JSONObject();
        val.put("value", vals[3]);
        key.put("keyword3", val);

        jsonObject1.put("data", key);
        logger.info("快递发货推送消息方法，推送json={}",jsonObject1.toJSONString());
        boolean pushResult = WechatUtil.setPush(jsonObject1.toString(), access_token);
        logger.info("快递发货推送消息方法结束：推送结果" + pushResult);
        return pushResult;
    }
}
