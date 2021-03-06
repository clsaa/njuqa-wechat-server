package com.clsaa.wechat.njuqa.server.service;

import com.clsaa.rest.result.Pagination;
import com.clsaa.rest.result.bizassert.BizAssert;
import com.clsaa.rest.result.bizassert.BizCode;
import com.clsaa.wechat.njuqa.server.config.BizCodes;
import com.clsaa.wechat.njuqa.server.config.NjuqaProperties;
import com.clsaa.wechat.njuqa.server.dao.UserDao;
import com.clsaa.wechat.njuqa.server.model.dto.WechatLoginUserDtoV1;
import com.clsaa.wechat.njuqa.server.model.po.User;
import com.clsaa.wechat.njuqa.server.model.vo.UserV1;
import com.clsaa.wechat.njuqa.server.util.BeanUtils;
import com.clsaa.wechat.njuqa.server.util.TimestampUtil;
import com.clsaa.wechat.njuqa.server.util.UUIDUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;


/**
 * @author joyren
 */
@Service
public class UserService {

    @Autowired
    private UserDao userDao;

    @Autowired
    private WechatService wechatService;

    @Autowired
    private NjuqaProperties njuqaProperties;

    @Transactional(rollbackFor = Exception.class, isolation = Isolation.SERIALIZABLE)
    public UserV1 addUser(String openid, String nickname, String avatarUrl) {
        User existUser = this.userDao.findByOpenId(openid);
        BizAssert.allowed(existUser == null,
                new BizCode(BizCodes.INVALID_PARAM.getCode(), "用户已注册"));
        User user = new User();
        user.setId(UUIDUtil.getUUID());
        user.setOpenId(openid);
        user.setNickname(nickname == null ? "匿名用户" : nickname);
        user.setAvatarUrl(avatarUrl == null ? "../../images/icon1.jpeg" : avatarUrl);
        user.setCtime(TimestampUtil.now());
        user.setMtime(TimestampUtil.now());
        user.setIdentity(User.IDENTITY_NORMAL_USER);
        User u = this.userDao.saveAndFlush(user);
        return BeanUtils.convertType(u, UserV1.class);
    }

    public boolean deleteUserById(String id) {
        User existUser = this.userDao.findUsersById(id);
        BizAssert.found(existUser != null, BizCodes.NOT_FOUND);
        this.userDao.delete(existUser);
        return true;
    }

    public UserV1 updateUser(String id, String nickname, String avatarUrl) {
        User existUser = this.userDao.findUsersById(id);
        BizAssert.found(existUser != null, BizCodes.NOT_FOUND);
        existUser.setNickname(nickname == null ? "匿名用户" : nickname);
        existUser.setAvatarUrl(avatarUrl == null ? "../../images/icon1.jpeg" : avatarUrl);
        existUser.setMtime(TimestampUtil.now());
        existUser = this.userDao.save(existUser);
        return BeanUtils.convertType(existUser, UserV1.class);
    }

    public UserV1 findUserV1ById(String id) {
        User existUser = this.userDao.findUsersById(id);
        BizAssert.found(existUser != null, BizCodes.NOT_FOUND);
        return BeanUtils.convertType(existUser, UserV1.class);
    }

    public Pagination<UserV1> getUserV1Pagination(Integer pageNo, Integer pageSize) {
        int count = (int) this.userDao.count();

        Pagination<UserV1> pagination = new Pagination<>();
        pagination.setPageNo(pageNo);
        pagination.setPageSize(pageSize);
        pagination.setTotalCount(count);

        if (count == 0) {
            pagination.setPageList(Collections.emptyList());
            return pagination;
        }
        Sort sort = new Sort(Sort.Direction.DESC, "ctime");
        Pageable pageRequest = PageRequest.of(pagination.getPageNo() - 1, pagination.getPageSize(), sort);
        List<User> userList = this.userDao.findAll(pageRequest).getContent();
        pagination.setPageList(userList.stream().map(u -> BeanUtils.convertType(u, UserV1.class)).collect(Collectors.toList()));
        return pagination;
    }

    public UserV1 findUserV1ByCode(String code) {
        WechatLoginUserDtoV1 wechatLoginUserDtoV1 = this.wechatService.getUserInfoByCode(
                this.njuqaProperties.getWechat().getAppid(),
                this.njuqaProperties.getWechat().getSecret(),
                code,
                "authorization_code");
        BizAssert.allowed(wechatLoginUserDtoV1.getErrcode() == null,
                new BizCode(BizCodes.INVALID_PARAM.getCode(),
                        wechatLoginUserDtoV1.getErrcode() + ":" + wechatLoginUserDtoV1.getErrmsg()));
        User existUser = this.userDao.findByOpenId(wechatLoginUserDtoV1.getOpenid());
        if (existUser != null) {
            return BeanUtils.convertType(existUser, UserV1.class);
        }

        UserV1 user = new UserV1();
        user.setOpenId(wechatLoginUserDtoV1.getOpenid());
        return user;
    }

}
