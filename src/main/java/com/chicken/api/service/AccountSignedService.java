package com.chicken.api.service;

import com.chicken.api.dao.AccountSignedDao;
import com.chicken.api.model.AccountSigned;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * @author zhanglei
 * @date 2019-09-01 21:10
 */
@Service
public class AccountSignedService {

    @Autowired
    AccountSignedDao accountSignedDao;

    @Transactional(rollbackFor = Exception.class)
    public int deleteByPrimaryKey(Integer id) {
        return accountSignedDao.deleteByPrimaryKey(id);
    }

    @Transactional(rollbackFor = Exception.class)
    @Async
    public void insert(AccountSigned record) {
         accountSignedDao.insert(record);
    }

    public AccountSigned selectByPrimaryKey(Integer id) {
        return accountSignedDao.selectByPrimaryKey(id);
    }


    public List<AccountSigned> selectAll() {
        return accountSignedDao.selectAll();
    }


    @Transactional(rollbackFor = Exception.class)
    public int updateByPrimaryKey(AccountSigned record) {
        return accountSignedDao.updateByPrimaryKey(record);
    }

    public List<AccountSigned> selectByAccountSigned(AccountSigned accountSigned) {
        List<AccountSigned> userLists = accountSignedDao.selectByAccountSigned(accountSigned);
        return userLists;
    }
}
