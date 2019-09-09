package com.chicken.api.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.chicken.api.model.Dictionary;
import com.chicken.api.service.DictionaryService;
import com.chicken.api.util.CallResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * @author zhanglei
 * @date 2019-09-09 20:31
 */
@RestController
@RequestMapping("/mp")
public class NotifyController {

    @Autowired
    DictionaryService dictionaryService;

    /**
     * 获取大力丸排行榜
     *
     * @return
     */
    @RequestMapping(value = "/newsNotify", method = RequestMethod.GET)
    @ResponseBody
    public Object newsNotify() {

        Dictionary dictionary = new Dictionary();
        dictionary.setDictType("tg");
        dictionary.setStatus("1");
        List<Dictionary> list = this.dictionaryService.selectByDictionary(dictionary);
        JSONArray jsonArray = new JSONArray();
        for(Dictionary d:list){
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("title",d.getDictName());
            jsonObject.put("content",d.getDictContent());
            jsonArray.add(jsonObject);
        }

        return CallResult.success(jsonArray.toArray());
    }
}
